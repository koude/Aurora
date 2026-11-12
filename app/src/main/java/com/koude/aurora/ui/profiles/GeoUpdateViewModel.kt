package com.koude.aurora.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koude.aurora.data.settings.GeoResourceUpdater
import com.github.kr328.clash.design.R as DesignR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GeoUpdateUiState(val running: Boolean = false, val message: String? = null)

class GeoUpdateViewModel(private val updater: GeoResourceUpdater, private val context: Context) : ViewModel() {
    private val mutableState = MutableStateFlow(GeoUpdateUiState())
    val state: StateFlow<GeoUpdateUiState> = mutableState.asStateFlow()

    fun update() {
        if (mutableState.value.running) return
        mutableState.value = GeoUpdateUiState(running = true, message = context.getString(DesignR.string.aurora_geo_update_preparing))
        viewModelScope.launch {
            try {
                val result = updater.update { name, index, total ->
                    mutableState.value = GeoUpdateUiState(true, context.getString(DesignR.string.aurora_geo_update_progress, name, index, total))
                }
                val message = when {
                    result.failed.isEmpty() -> context.getString(DesignR.string.aurora_geo_update_success)
                    result.updated.isEmpty() -> context.getString(DesignR.string.aurora_geo_update_failed)
                    else -> context.getString(DesignR.string.aurora_geo_update_partial, result.updated.size, result.failed.joinToString())
                }
                mutableState.value = GeoUpdateUiState(message = message)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = GeoUpdateUiState(message = error.message ?: context.getString(DesignR.string.aurora_geo_update_failed))
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GeoUpdateViewModel(GeoResourceUpdater(context.applicationContext), context.applicationContext) as T
        }
    }
}
