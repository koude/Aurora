package com.koude.aurora.ui.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfilePropertiesRepository
import com.koude.aurora.data.profiles.ServiceProfilePropertiesRepository
import java.util.UUID

class ProfilePropertiesViewModel(private val repository: ProfilePropertiesRepository) : ViewModel() {
    private var original: Profile? = null
    var profile by mutableStateOf<Profile?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var progress by mutableStateOf<String?>(null)
        private set
    val hasUnsavedChanges: Boolean get() = profile != original
    private var released = false

    suspend fun load(uuid: UUID): Boolean {
        original = repository.query(uuid)
        profile = original
        released = false
        return profile != null
    }

    fun update(value: Profile) {
        profile = value
    }

    suspend fun save() {
        val current = requireNotNull(profile)
        require(current.name.isNotBlank()) { "配置名称不能为空" }
        require(current.type == Profile.Type.File || current.source.isNotBlank()) { "订阅地址不能为空" }
        saving = true
        progress = "正在验证配置…"
        try {
            repository.patch(current)
            repository.commit(current.uuid) { progress = statusLabel(it) }
        } finally {
            saving = false
            progress = null
        }
    }

    suspend fun autoSave() {
        val current = profile ?: return
        if (!released && current != original) repository.patch(current)
    }

    suspend fun release(uuid: UUID) {
        released = true
        repository.release(uuid)
    }

    private fun statusLabel(status: FetchStatus): String = when (status.action) {
        FetchStatus.Action.FetchConfiguration -> "正在下载配置…"
        FetchStatus.Action.FetchProviders -> "正在下载附属资源… ${status.progress}/${status.max}"
        FetchStatus.Action.SubscriptionInfo -> "正在读取订阅信息…"
        FetchStatus.Action.Verifying -> "正在验证配置… ${status.progress}/${status.max}"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfilePropertiesViewModel(ServiceProfilePropertiesRepository()) as T
        }
    }
}
