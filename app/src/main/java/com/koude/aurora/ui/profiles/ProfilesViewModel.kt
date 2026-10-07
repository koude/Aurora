package com.koude.aurora.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfileFileGateway
import com.koude.aurora.data.profiles.ProfileImportCoordinator
import com.koude.aurora.data.profiles.ProfileRepository
import com.koude.aurora.data.profiles.ProfileRepositoryProvider
import com.koude.aurora.data.profiles.ServiceProfileFileGateway
import com.koude.aurora.data.profiles.ServiceProfileImportGateway
import com.koude.aurora.model.ProfileSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ProfilesUiState(
    val loading: Boolean = true,
    val profiles: List<ProfileSummary> = emptyList(),
    val errorMessage: String? = null,
)

data class ProfileActionError(val cause: Throwable, val title: String)

class ProfilesViewModel(
    private val repository: ProfileRepository,
    private val importCoordinator: ProfileImportCoordinator,
    private val fileGateway: ProfileFileGateway,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = mutableUiState.asStateFlow()
    private val mutableImportProgress = MutableStateFlow<ProfileImportProgress?>(null)
    val importProgress: StateFlow<ProfileImportProgress?> = mutableImportProgress.asStateFlow()
    private val errorChannel = Channel<ProfileActionError>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    private val duplicateChannel = Channel<UUID>(Channel.BUFFERED)
    val duplicatedProfiles = duplicateChannel.receiveAsFlow()
    @Volatile private var importInProgress = false
    @Volatile private var importGeneration = 0

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

    fun refresh() = perform { repository.refresh() }

    fun update(id: UUID) = perform { repository.update(id) }

    fun updateAll() = perform { repository.updateAll() }

    fun delete(id: UUID) = perform { repository.delete(id) }

    fun importUrl(name: String, url: String) = import(Profile.Type.Url, name, url) { }

    fun importFile(name: String, sourceUri: String) = import(Profile.Type.File, name, "") { id ->
        fileGateway.copyConfiguration(id, sourceUri)
    }

    private fun import(type: Profile.Type, name: String, source: String, prepare: suspend (UUID) -> Unit) {
        if (importInProgress) return
        importInProgress = true
        val generation = ++importGeneration
        mutableImportProgress.value = ProfileImportProgress(
            stage = if (type == Profile.Type.Url) "正在连接订阅并准备下载…" else "正在导入配置…",
        )
        viewModelScope.launch {
            try {
                val imported = importCoordinator.import(type, name, source, prepare) { status ->
                    if (status.action != FetchStatus.Action.SubscriptionInfo &&
                        generation == importGeneration && importInProgress
                    ) {
                        mutableImportProgress.value = status.toProfileImportProgress()
                    }
                }
                if (imported) refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorChannel.send(ProfileActionError(
                    error,
                    if (type == Profile.Type.Url) "订阅获取失败" else "配置导入失败",
                ))
                refresh()
            } finally {
                if (generation == importGeneration) {
                    importInProgress = false
                    mutableImportProgress.value = null
                }
            }
        }
    }

    fun duplicate(id: UUID) {
        viewModelScope.launch {
            try {
                val copyId = repository.duplicate(id)
                duplicateChannel.send(copyId)
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                errorChannel.send(ProfileActionError(error, "操作失败"))
            }
        }
    }

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
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfilesViewModel(
                    ProfileRepositoryProvider.instance,
                    ProfileImportCoordinator(ServiceProfileImportGateway()),
                    ServiceProfileFileGateway(context.applicationContext),
                ) as T
            }
        }
    }
}
