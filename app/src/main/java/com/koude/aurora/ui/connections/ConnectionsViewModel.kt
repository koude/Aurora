package com.koude.aurora.ui.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.ConnectionInfo
import com.koude.aurora.data.connections.ConnectionsRepository
import com.koude.aurora.data.connections.ServiceConnectionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel(
    private val repository: ConnectionsRepository,
    private val nowNanos: () -> Long = System::nanoTime,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = mutableUiState.asStateFlow()
    private var refreshJob: Job? = null
    private var refreshGeneration = 0
    private var serviceRunning = false
    private var previousConnections: Map<String, ConnectionInfo> = emptyMap()
    private var previousSampleNanos: Long? = null

    fun refresh(serviceRunning: Boolean, force: Boolean = false) {
        this.serviceRunning = serviceRunning
        if (!serviceRunning) {
            refreshGeneration++
            refreshJob?.cancel()
            refreshJob = null
            previousConnections = emptyMap()
            previousSampleNanos = null
            mutableUiState.value = ConnectionsUiState()
            return
        }
        if (refreshJob?.isActive == true && !force) return
        val generation = ++refreshGeneration
        refreshJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            serviceRunning = true,
            loading = mutableUiState.value.connections.isEmpty(),
            errorMessage = null,
        )
        refreshJob = viewModelScope.launch {
            try {
                val snapshot = repository.query()
                if (generation != refreshGeneration || !this@ConnectionsViewModel.serviceRunning) return@launch
                val sampledAt = nowNanos()
                val elapsedNanos = previousSampleNanos?.let { sampledAt - it } ?: 0L
                val speeds = if (elapsedNanos > 0L) {
                    snapshot.connections.mapNotNull { connection ->
                        val previous = previousConnections[connection.id] ?: return@mapNotNull null
                        connection.id to ConnectionSpeed(
                            uploadedBytesPerSecond = bytesPerSecond(connection.uploaded - previous.uploaded, elapsedNanos),
                            downloadedBytesPerSecond = bytesPerSecond(connection.downloaded - previous.downloaded, elapsedNanos),
                        )
                    }.toMap()
                } else emptyMap()
                previousConnections = snapshot.connections.associateBy(ConnectionInfo::id)
                previousSampleNanos = sampledAt
                mutableUiState.value = ConnectionsUiState(
                    serviceRunning = true,
                    connections = snapshot.connections,
                    appLabels = snapshot.appLabels,
                    speeds = speeds,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation != refreshGeneration || !this@ConnectionsViewModel.serviceRunning) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    loading = false,
                    errorMessage = error.message ?: "连接读取失败",
                )
            }
        }
    }

    fun close(ids: List<String>, onError: (Throwable) -> Unit) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.close(ids)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError(error)
            }
            refresh(serviceRunning, force = true)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ConnectionsViewModel(ServiceConnectionsRepository()) as T
        }
    }
}

private fun bytesPerSecond(deltaBytes: Long, elapsedNanos: Long): Long =
    (deltaBytes.coerceAtLeast(0).toDouble() * 1_000_000_000.0 / elapsedNanos)
        .toLong()
        .coerceAtLeast(0)
