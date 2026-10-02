package com.koude.aurora.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koude.aurora.data.profiles.ProfileRepository
import com.koude.aurora.data.profiles.ProfileRepositoryProvider
import com.koude.aurora.model.ProfileSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

data class ProfilesUiState(
    val loading: Boolean = true,
    val profiles: List<ProfileSummary> = emptyList(),
    val errorMessage: String? = null,
)

class ProfilesViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profiles
                .catch { error ->
                    mutableUiState.value = ProfilesUiState(
                        loading = false,
                        errorMessage = error.message,
                    )
                }
                .collect { profiles ->
                    mutableUiState.value = ProfilesUiState(
                        loading = false,
                        profiles = profiles,
                    )
                }
        }
    }

    fun activate(id: UUID) = perform { repository.activate(id) }

    fun update(id: UUID) = perform { repository.update(id) }

    fun delete(id: UUID) = perform { repository.delete(id) }

    private fun perform(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onFailure { error ->
                    mutableUiState.value = mutableUiState.value.copy(
                        errorMessage = error.message,
                    )
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfilesViewModel(ProfileRepositoryProvider.instance) as T
            }
        }
    }
}
