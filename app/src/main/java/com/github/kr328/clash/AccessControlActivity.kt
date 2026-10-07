package com.github.kr328.clash

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.getSystemService
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.AccessControlScreen
import com.koude.aurora.ui.settings.AccessControlViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AccessControlActivity : BaseActivity() {
    private val editor: AccessControlViewModel by viewModels { AccessControlViewModel.factory(this) }

    override suspend fun main() {
        editor.load()

        setContent {
            AuroraTheme {
                AccessControlScreen(
                    apps = editor.apps,
                    mode = editor.mode,
                    selectedPackages = editor.selectedPackages,
                    hasUnsavedChanges = editor.hasUnsavedChanges,
                    applying = editor.applying,
                    showSystemApps = editor.options?.showSystemApps ?: false,
                    sort = editor.options?.sort ?: com.github.kr328.clash.design.model.AppInfoSort.Label,
                    reverse = editor.options?.reverse ?: false,
                    onModeChanged = editor::changeMode,
                    onToggleApp = editor::toggle,
                    onSelectAll = editor::selectAll,
                    onSelectNone = editor::selectNone,
                    onSelectInvert = editor::selectInvert,
                    onShowSystemAppsChanged = { launch { editor.setShowSystemApps(it) } },
                    onSortChanged = { launch { editor.setSort(it) } },
                    onReverseChanged = { launch { editor.setReverse(it) } },
                    onImport = ::importPackages,
                    onExport = ::exportPackages,
                    onApply = {
                        if (editor.beginApply()) launch {
                            val message = editor.apply(clashRunning && editor.options?.vpnEnabled == true) {
                                stopClashService()
                                val stopped = withTimeoutOrNull(15_000) {
                                    while (clashRunning) delay(200)
                                    true
                                } == true
                                if (!stopped) error("VPN stop timed out")
                                if (startClashService() != null) error("VPN permission required")
                            }
                            if (message != null) Toast.makeText(this@AccessControlActivity, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    onBack = ::finish,
                )
            }
        }
    }

    private fun importPackages() {
        val data = getSystemService<ClipboardManager>()?.primaryClip ?: return
        if (data.itemCount == 0) return
        val names = data.getItemAt(0).text?.split('\n')?.toSet().orEmpty()
        editor.import(names)
    }

    private fun exportPackages() {
        getSystemService<ClipboardManager>()?.setPrimaryClip(
            ClipData.newPlainText("packages", editor.export()),
        )
    }
}
