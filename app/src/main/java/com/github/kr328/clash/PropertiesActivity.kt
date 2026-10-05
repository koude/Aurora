package com.github.kr328.clash

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.profiles.ProfilePropertiesScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class PropertiesActivity : BaseActivity<Design<*>>() {
    private var canceled: Boolean = false
    private lateinit var original: Profile
    private lateinit var profileState: androidx.compose.runtime.MutableState<Profile>
    private val savingState = mutableStateOf(false)
    private val progressState = mutableStateOf<String?>(null)

    override suspend fun main() {
        setResult(RESULT_CANCELED)
        val uuid = intent.uuid ?: return finish()
        original = withProfile { queryByUUID(uuid) } ?: return finish()
        profileState = mutableStateOf(original)

        setContent {
            AuroraTheme {
                ProfilePropertiesScreen(
                    profile = profileState.value,
                    hasUnsavedChanges = profileState.value != original,
                    saving = savingState.value,
                    progress = progressState.value,
                    onProfileChange = { profileState.value = it },
                    onSave = { launch { commitProfile() } },
                    onBrowseFiles = { startActivity(FilesActivity::class.intent.setUUID(uuid)) },
                    onBack = ::finish,
                )
            }
        }

        defer {
            canceled = true
            withProfile { release(uuid) }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStop -> {
                            val profile = profileState.value
                            if (!canceled && profile != original) {
                                withProfile { patch(profile.uuid, profile.name, profile.source, profile.interval, profile.ageSecretKey) }
                            }
                        }
                        Event.ServiceRecreated -> finish()
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun commitProfile() {
        val profile = profileState.value
        if (profile.name.isBlank()) {
            Toast.makeText(this, "配置名称不能为空", Toast.LENGTH_LONG).show()
            return
        }
        if (profile.type != Profile.Type.File && profile.source.isBlank()) {
            Toast.makeText(this, "订阅地址不能为空", Toast.LENGTH_LONG).show()
            return
        }
        savingState.value = true
        progressState.value = "正在验证配置…"
        try {
            withProfile {
                patch(profile.uuid, profile.name, profile.source, profile.interval, profile.ageSecretKey)
                coroutineScope {
                    launch {
                        commit(profile.uuid) { status ->
                            progressState.value = statusLabel(status)
                        }
                    }
                }
            }
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "保存配置失败", Toast.LENGTH_LONG).show()
        } finally {
            savingState.value = false
            progressState.value = null
        }
    }

    private fun statusLabel(status: FetchStatus): String = when (status.action) {
        FetchStatus.Action.FetchConfiguration -> "正在下载配置…"
        FetchStatus.Action.FetchProviders -> "正在下载附属资源… ${status.progress}/${status.max}"
        FetchStatus.Action.SubscriptionInfo -> "正在读取订阅信息…"
        FetchStatus.Action.Verifying -> "正在验证配置… ${status.progress}/${status.max}"
    }
}
