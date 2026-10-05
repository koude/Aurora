package com.koude.aurora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.RoutePreview
import com.github.kr328.clash.core.model.TunnelState
import com.koude.aurora.data.home.WebsiteLatencyRepository
import com.koude.aurora.data.home.WebsiteLatencyRepositoryProvider
import com.koude.aurora.model.WebsiteLatencySite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

class RouteTestRequest internal constructor(
    val target: String,
    internal val id: Long,
)

class HomeViewModel(
    private val latencyRepository: WebsiteLatencyRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()
    private val latencyJobs = mutableMapOf<WebsiteLatencySite, Job>()
    private val mutableRouteTestState = MutableStateFlow(RouteTestUiState())
    val routeTestState: StateFlow<RouteTestUiState> = mutableRouteTestState.asStateFlow()
    private var nextRouteTestRequestId = 0L
    private var activeRouteTestRequestId: Long? = null

    fun updateConnection(running: Boolean, mode: TunnelState.Mode, profileName: String?) {
        mutableUiState.update { it.copy(running = running, mode = mode, profileName = profileName) }
    }

    fun updateTraffic(uploadSpeed: String, downloadSpeed: String) {
        mutableUiState.update { it.copy(uploadSpeed = uploadSpeed, downloadSpeed = downloadSpeed) }
    }

    fun updateMode(mode: TunnelState.Mode) {
        mutableUiState.update { it.copy(mode = mode) }
    }

    fun clearTraffic() {
        updateTraffic("-- B/s", "-- B/s")
    }

    fun openRouteTest() {
        activeRouteTestRequestId = null
        mutableRouteTestState.update {
            it.copy(isOpen = true, isTesting = false, errorMessage = null, preview = null)
        }
    }

    fun dismissRouteTest() {
        activeRouteTestRequestId = null
        mutableRouteTestState.update { it.copy(isOpen = false, isTesting = false) }
    }

    fun cancelRouteTestRequest() {
        activeRouteTestRequestId = null
        mutableRouteTestState.update { it.copy(isTesting = false) }
    }

    fun changeRouteTestTarget(target: String) {
        mutableRouteTestState.update {
            it.copy(target = target, errorMessage = null, preview = null)
        }
    }

    fun prepareRouteTest(
        serviceRunning: Boolean,
        invalidTargetMessage: String,
        serviceRequiredMessage: String,
    ): RouteTestRequest? {
        val state = mutableRouteTestState.value
        val target = normalizeRouteTarget(state.target.trim())
        if (target == null) {
            mutableRouteTestState.update {
                it.copy(errorMessage = invalidTargetMessage, preview = null)
            }
            return null
        }
        if (!serviceRunning) {
            mutableRouteTestState.update {
                it.copy(errorMessage = serviceRequiredMessage, preview = null)
            }
            return null
        }

        val request = RouteTestRequest(target, ++nextRouteTestRequestId)
        activeRouteTestRequestId = request.id
        mutableRouteTestState.update {
            it.copy(
                target = target,
                isTesting = true,
                errorMessage = null,
                preview = null,
            )
        }
        return request
    }

    fun completeRouteTest(request: RouteTestRequest, preview: RoutePreview) {
        mutableRouteTestState.update { state ->
            if (!isCurrentRouteTestRequest(state, request)) state
            else state.copy(
                isTesting = false,
                errorMessage = preview.error,
                preview = preview.takeIf { it.error == null },
            )
        }
    }

    fun failRouteTest(request: RouteTestRequest, errorMessage: String) {
        mutableRouteTestState.update { state ->
            if (!isCurrentRouteTestRequest(state, request)) state
            else state.copy(isTesting = false, errorMessage = errorMessage, preview = null)
        }
    }

    private fun isCurrentRouteTestRequest(state: RouteTestUiState, request: RouteTestRequest): Boolean =
        state.isOpen && state.isTesting && state.target == request.target &&
            activeRouteTestRequestId == request.id

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
                HomeViewModel(WebsiteLatencyRepositoryProvider.instance) as T
        }
    }
}
