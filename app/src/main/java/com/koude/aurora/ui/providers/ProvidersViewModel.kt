package com.koude.aurora.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.Provider
import com.koude.aurora.data.providers.ProvidersRepository
import com.koude.aurora.data.providers.ServiceProvidersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderUpdateError(val name: String, val message: String)

class ProvidersViewModel(
    private val repository: ProvidersRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableRows = MutableStateFlow<List<ProviderRowState>>(emptyList())
    val rows: StateFlow<List<ProviderRowState>> = mutableRows.asStateFlow()
    private val errorChannel = Channel<ProviderUpdateError>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()

    suspend fun refresh() {
        try {
            mutableRows.value = repository.query().map { ProviderRowState(it, it.updatedAt) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            errorChannel.send(ProviderUpdateError("", error.message.orEmpty()))
        }
    }

    fun update(index: Int) {
        val row = rows.value.getOrNull(index) ?: return
        if (row.updating || row.provider.vehicleType == Provider.VehicleType.Inline) return
        val key = row.provider.name to row.provider.type
        updateRow(key) { it.copy(updating = true) }

        viewModelScope.launch {
            try {
                repository.update(row.provider.type, row.provider.name)
                updateRow(key) { it.copy(updating = false, updatedAt = now()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateRow(key) { it.copy(updating = false) }
                errorChannel.send(ProviderUpdateError(row.provider.name, error.message.orEmpty()))
            }
        }
    }

    private fun updateRow(key: Pair<String, Provider.Type>, transform: (ProviderRowState) -> ProviderRowState) {
        mutableRows.update { rows ->
            rows.map { row ->
                if (row.provider.name == key.first && row.provider.type == key.second) transform(row) else row
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProvidersViewModel(ServiceProvidersRepository()) as T
        }
    }
}
