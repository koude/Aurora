package com.koude.aurora.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.design.model.DarkMode
import com.koude.aurora.data.settings.AppSettingsRepository
import com.koude.aurora.data.settings.StoredAppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppSettingsViewModel(private val repository: AppSettingsRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = mutableUiState.asStateFlow()

    fun refresh(running: Boolean) {
        val settings = repository.read()
        mutableUiState.value = AppSettingsUiState(
            autoRestart = settings.autoRestart,
            darkMode = settings.darkMode,
            dynamicColor = settings.dynamicColor,
            hideAppIcon = settings.hideAppIcon,
            hideFromRecents = settings.hideFromRecents,
            showTraffic = settings.showTraffic,
            running = running,
        )
    }

    fun setAutoRestart(value: Boolean) {
        repository.setAutoRestart(value)
        mutableUiState.update { it.copy(autoRestart = value) }
    }

    fun setDarkMode(value: DarkMode) {
        repository.setDarkMode(value)
        mutableUiState.update { it.copy(darkMode = value) }
    }

    fun setDynamicColor(value: Boolean) {
        repository.setDynamicColor(value)
        mutableUiState.update { it.copy(dynamicColor = value) }
    }

    fun setHideAppIcon(value: Boolean) {
        repository.setHideAppIcon(value)
        mutableUiState.update { it.copy(hideAppIcon = value) }
    }

    fun setHideFromRecents(value: Boolean) {
        repository.setHideFromRecents(value)
        mutableUiState.update { it.copy(hideFromRecents = value) }
    }

    fun setShowTraffic(value: Boolean) {
        repository.setShowTraffic(value)
        mutableUiState.update { it.copy(showTraffic = value) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppSettingsViewModel(StoredAppSettingsRepository(context.applicationContext)) as T
        }
    }
}
