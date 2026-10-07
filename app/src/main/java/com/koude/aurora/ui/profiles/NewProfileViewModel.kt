package com.koude.aurora.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfileCreationRepository
import com.koude.aurora.data.profiles.ServiceProfileCreationRepository
import java.util.UUID

class NewProfileViewModel(private val repository: ProfileCreationRepository) : ViewModel() {
    suspend fun createFile(name: String): UUID = repository.create(Profile.Type.File, name)
    suspend fun createUrl(name: String, url: String? = null): UUID = repository.create(Profile.Type.Url, name, url)
    suspend fun createExternal(name: String, url: String): UUID = repository.create(Profile.Type.External, name, url)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NewProfileViewModel(ServiceProfileCreationRepository()) as T
        }
    }
}
