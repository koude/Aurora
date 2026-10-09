@file:Suppress("BlockingMethodInNonBlockingContext")

package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.grantPermissions
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.uuid
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.profiles.FilesScreen
import com.koude.aurora.ui.profiles.ProfileFilesViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class FilesActivity : BaseActivity() {
    private val browser: ProfileFilesViewModel by viewModels { ProfileFilesViewModel.factory(this) }

    override suspend fun main() {
        val uuid = intent.uuid ?: return finish()
        if (!browser.load(uuid)) return finish()
        setContent {
            val screenState by browser.uiState.collectAsStateWithLifecycle()
            AuroraTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                FilesScreen(
                    files = screenState.files,
                    currentInBase = screenState.inBase,
                    configurationEditable = screenState.editable,
                    onBack = { if (screenState.inBase) finish() else launch { browser.back() } },
                    onOpenDirectory = { file -> launch { browser.enter(file.id) } },
                    onOpenFile = { file -> startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(browser.documentUri(file), "text/plain").grantPermissions()) },
                    onImport = { file, uri, name -> launch {
                        try {
                            browser.import(file, uri, name)
                        } catch (e: Exception) { showFailure(e) }
                    } },
                    onExport = { file, uri -> launch {
                        try { browser.export(file, uri) }
                        catch (e: Exception) { showFailure(e) }
                    } },
                    onRename = { file, name -> launch {
                        try { browser.rename(file, name) }
                        catch (e: Exception) { showFailure(e) }
                    } },
                    onDelete = { file -> launch {
                        try { browser.delete(file) }
                        catch (e: Exception) { showFailure(e) }
                    } },
                )
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ActivityStop -> runCatching { browser.refresh() }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showFailure(error: Exception) {
        Toast.makeText(this, error.message ?: error.javaClass.simpleName, Toast.LENGTH_LONG).show()
    }
}
