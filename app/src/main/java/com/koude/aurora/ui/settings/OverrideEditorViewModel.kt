package com.koude.aurora.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.net.Uri
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.koude.aurora.data.settings.OverrideRepository
import com.koude.aurora.data.settings.ServiceOverrideRepository
import com.koude.aurora.data.settings.GeoImportRepository
import com.koude.aurora.data.settings.GeoImportResult
import com.koude.aurora.data.settings.LocalGeoImportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OverrideEditorViewModel(
    private val repository: OverrideRepository,
    private val geoImporter: GeoImportRepository? = null,
) : ViewModel() {
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

    suspend fun importGeo(uri: Uri, kind: String): GeoImportResult =
        requireNotNull(geoImporter).import(uri, kind)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OverrideEditorViewModel(ServiceOverrideRepository()) as T
        }

        fun factoryWithGeo(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OverrideEditorViewModel(ServiceOverrideRepository(), LocalGeoImportRepository(context.applicationContext)) as T
        }
    }
}
