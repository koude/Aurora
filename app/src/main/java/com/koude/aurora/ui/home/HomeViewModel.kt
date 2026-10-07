package com.koude.aurora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.TunnelState
import com.koude.aurora.data.home.HomeRepository
import com.koude.aurora.data.home.ServiceHomeRepository
import com.koude.aurora.data.home.WebsiteLatencyRepository
import com.koude.aurora.data.home.WebsiteLatencyRepositoryProvider
import com.koude.aurora.model.WebsiteLatencySite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

class HomeViewModel(
    private val latencyRepository: WebsiteLatencyRepository,
    private val repository: HomeRepository = ServiceHomeRepository(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()
    private val latencyJobs = mutableMapOf<WebsiteLatencySite, Job>()
    private val errorChannel = Channel<Throwable>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    private var connectionJob: Job? = null
    private var trafficJob: Job? = null
    private var connectionGeneration = 0
    private var trafficGeneration = 0
    private val mutableRouteTestState = MutableStateFlow(RouteTestUiState())
    val routeTestState: StateFlow<RouteTestUiState> = mutableRouteTestState.asStateFlow()
    private var routePreviewJob: Job? = null
    private var nextRouteTestRequestId = 0L
    private var activeRouteTestRequestId: Long? = null

    fun refreshConnection(running: Boolean) {
        connectionJob?.cancel()
        val generation = ++connectionGeneration
        if (!running) mutableUiState.update { it.copy(running = false) }
        connectionJob = viewModelScope.launch {
            try {
                val connection = repository.connection()
                if (generation == connectionGeneration) {
                    mutableUiState.update {
                        it.copy(running = running, mode = connection.mode, profileName = connection.profileName)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == connectionGeneration) errorChannel.send(error)
            }
        }
    }

    fun refreshTraffic() {
        if (!uiState.value.running || trafficJob?.isActive == true) return
        val generation = trafficGeneration
        trafficJob = viewModelScope.launch {
            try {
                val traffic = repository.traffic()
                if (generation == trafficGeneration && uiState.value.running) {
                    mutableUiState.update {
                        it.copy(uploadSpeed = traffic.uploadSpeed, downloadSpeed = traffic.downloadSpeed)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A single failed sample must not stop the periodic traffic loop.
            }
        }
    }

    fun onServiceStopped() {
        cancelRouteTestRequest()
        trafficGeneration++
        trafficJob?.cancel()
        mutableUiState.update {
            it.copy(running = false, uploadSpeed = "-- B/s", downloadSpeed = "-- B/s")
        }
        refreshConnection(false)
    }

    fun setMode(mode: TunnelState.Mode) {
        viewModelScope.launch {
            try {
                repository.setMode(mode)
                mutableUiState.update { it.copy(mode = mode) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorChannel.send(error)
            }
        }
    }

    fun openRouteTest() {
        routePreviewJob?.cancel()
        activeRouteTestRequestId = null
        mutableRouteTestState.update {
            it.copy(isOpen = true, isTesting = false, errorMessage = null, preview = null)
        }
    }

    fun dismissRouteTest() {
        routePreviewJob?.cancel()
        activeRouteTestRequestId = null
        mutableRouteTestState.update { it.copy(isOpen = false, isTesting = false) }
    }

    fun cancelRouteTestRequest() {
        routePreviewJob?.cancel()
        activeRouteTestRequestId = null
        mutableRouteTestState.update { it.copy(isTesting = false) }
    }

    fun changeRouteTestTarget(target: String) {
        routePreviewJob?.cancel()
        activeRouteTestRequestId = null
        mutableRouteTestState.update {
            it.copy(target = target, isTesting = false, errorMessage = null, preview = null)
        }
    }

    fun submitRouteTest(
        serviceRunning: Boolean,
        invalidTargetMessage: String,
        serviceRequiredMessage: String,
        testFailedMessage: String,
    ) {
        routePreviewJob?.cancel()
        activeRouteTestRequestId = null
        val state = mutableRouteTestState.value
        val target = normalizeRouteTarget(state.target.trim())
        if (target == null) {
            mutableRouteTestState.update {
                it.copy(isTesting = false, errorMessage = invalidTargetMessage, preview = null)
            }
            return
        }
        if (!serviceRunning) {
            mutableRouteTestState.update {
                it.copy(isTesting = false, errorMessage = serviceRequiredMessage, preview = null)
            }
            return
        }

        val requestId = ++nextRouteTestRequestId
        activeRouteTestRequestId = requestId
        mutableRouteTestState.update {
            it.copy(
                target = target,
                isTesting = true,
                errorMessage = null,
                preview = null,
            )
        }
        routePreviewJob = viewModelScope.launch {
            try {
                val preview = repository.previewRoute(target)
                mutableRouteTestState.update { current ->
                    if (!isCurrentRouteTestRequest(current, target, requestId)) current
                    else current.copy(
                        isTesting = false,
                        errorMessage = preview.error,
                        preview = preview.takeIf { it.error == null },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableRouteTestState.update { current ->
                    if (!isCurrentRouteTestRequest(current, target, requestId)) current
                    else current.copy(isTesting = false, errorMessage = testFailedMessage, preview = null)
                }
            }
        }
    }

    private fun isCurrentRouteTestRequest(state: RouteTestUiState, target: String, id: Long): Boolean =
        state.isOpen && state.isTesting && state.target == target && activeRouteTestRequestId == id

    fun testAllSiteLatencies() {
        mutableUiState.update { it.copy(latencyTesting = true) }
        WebsiteLatencySite.entries.forEach(::startSiteLatencyTest)
    }

    fun testSiteLatency(site: WebsiteLatencySite) = startSiteLatencyTest(site)

    private fun startSiteLatencyTest(site: WebsiteLatencySite) {
        if (latencyJobs[site]?.isActive == true) return

        mutableUiState.update { state ->
            updateSiteLatency(
                state.copy(testingLatencySites = state.testingLatencySites + site),
                site,
                "检测中",
            )
        }
        latencyJobs[site] = viewModelScope.launch {
            try {
                val latency = try {
                    latencyRepository.measure(site)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                mutableUiState.update { state ->
                    updateSiteLatency(state, site, latency?.let { "$it ms" } ?: "超时")
                }
            } finally {
                latencyJobs.remove(site)
                mutableUiState.update { state ->
                    val remaining = state.testingLatencySites - site
                    state.copy(
                        testingLatencySites = remaining,
                        latencyTesting = state.latencyTesting && remaining.isNotEmpty(),
                    )
                }
            }
        }
    }

    private fun updateSiteLatency(state: HomeUiState, site: WebsiteLatencySite, value: String) = when (site) {
        WebsiteLatencySite.Apple -> state.copy(appleLatency = value)
        WebsiteLatencySite.GitHub -> state.copy(githubLatency = value)
        WebsiteLatencySite.YouTube -> state.copy(youtubeLatency = value)
        WebsiteLatencySite.Google -> state.copy(googleLatency = value)
    }

    private fun normalizeRouteTarget(value: String): String? {
        if (value.isBlank() || value.any(Char::isWhitespace)) return null
        return runCatching {
            val uri = URI(if ("://" in value) value else "https://$value")
            uri.host?.trimEnd('.')?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(WebsiteLatencyRepositoryProvider.instance, ServiceHomeRepository()) as T
        }
    }
}
