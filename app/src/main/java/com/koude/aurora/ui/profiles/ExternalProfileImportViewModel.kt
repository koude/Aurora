package com.koude.aurora.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ExternalProfileImportRepository
import com.koude.aurora.data.profiles.ServiceExternalProfileImportRepository
import java.util.UUID
import java.util.concurrent.TimeUnit

class ExternalProfileImportViewModel(private val repository: ExternalProfileImportRepository) : ViewModel() {
    suspend fun importProfile(type: String?, name: String, source: String, intervalMinutes: String?): UUID {
        val profileType = if (type.equals("file", ignoreCase = true)) Profile.Type.File else Profile.Type.Url
        val parsedInterval = intervalMinutes?.toLongOrNull() ?: 0L
        val updateInterval = if (parsedInterval > 0) parsedInterval.coerceAtLeast(15L) else 0L
        return repository.createAndPatch(profileType, name, source, TimeUnit.MINUTES.toMillis(updateInterval))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExternalProfileImportViewModel(ServiceExternalProfileImportRepository()) as T
        }
    }
}
