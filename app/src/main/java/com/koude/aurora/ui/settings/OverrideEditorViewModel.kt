package com.koude.aurora.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.koude.aurora.data.settings.OverrideRepository
import com.koude.aurora.data.settings.ServiceOverrideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OverrideEditorViewModel(private val repository: OverrideRepository) : ViewModel() {
    private var configuration: ConfigurationOverride? = null
    private val mutableRevision = MutableStateFlow(0)
    val revision: StateFlow<Int> = mutableRevision.asStateFlow()

    suspend fun load(): ConfigurationOverride = repository.query().also { configuration = it }

    fun changed() {
        mutableRevision.update { it + 1 }
    }

    suspend fun save() {
        configuration?.let { repository.patch(it) }
    }

    suspend fun reset() {
        configuration = null
        repository.clear()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OverrideEditorViewModel(ServiceOverrideRepository()) as T
        }
    }
}
