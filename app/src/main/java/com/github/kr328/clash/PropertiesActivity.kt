package com.github.kr328.clash

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.uuid
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.profiles.ProfilePropertiesScreen
import com.koude.aurora.ui.profiles.ProfilePropertiesViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class PropertiesActivity : BaseActivity() {
    private val editor: ProfilePropertiesViewModel by viewModels { ProfilePropertiesViewModel.Factory }

    override suspend fun main() {
        setResult(RESULT_CANCELED)
        val uuid = intent.uuid ?: return finish()
        if (!editor.load(uuid)) return finish()

        setContent {
            AuroraTheme(darkTheme = isDarkTheme) {
                editor.profile?.let { current -> ProfilePropertiesScreen(
                    profile = current,
                    hasUnsavedChanges = editor.hasUnsavedChanges,
                    saving = editor.saving,
                    progress = editor.progress,
                    onProfileChange = editor::update,
                    onSave = { launch { commitProfile() } },
                    onBrowseFiles = { startActivity(FilesActivity::class.intent.setUUID(uuid)) },
                    onBack = ::finish,
                ) }
            }
        }

        defer {
            editor.release(uuid)
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStop -> editor.autoSave()
                        Event.ServiceRecreated -> finish()
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun commitProfile() {
        try {
            editor.save()
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "保存配置失败", Toast.LENGTH_LONG).show()
        }
    }
}
