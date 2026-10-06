package com.koude.aurora.ui.connections

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koude.aurora.data.connections.ConnectionsRepository
import com.koude.aurora.data.connections.ServiceConnectionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel(private val repository: ConnectionsRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = mutableUiState.asStateFlow()
    private var refreshJob: Job? = null
    private var refreshGeneration = 0
    private var serviceRunning = false

    fun refresh(serviceRunning: Boolean, force: Boolean = false) {
        this.serviceRunning = serviceRunning
        if (!serviceRunning) {
            refreshGeneration++
            refreshJob?.cancel()
            refreshJob = null
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
                mutableUiState.value = ConnectionsUiState(
                    serviceRunning = true,
                    connections = snapshot.connections,
                    appLabels = snapshot.appLabels,
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
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ConnectionsViewModel(ServiceConnectionsRepository(context.applicationContext)) as T
        }
    }
}
