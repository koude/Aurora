@file:Suppress("BlockingMethodInNonBlockingContext")

package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.github.kr328.clash.common.util.grantPermissions
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.design.FilesDesign
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.profiles.FilesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.Stack

class FilesActivity : BaseActivity<FilesDesign>() {
    private val filesState = mutableStateOf<List<File>>(emptyList())
    private val currentInBaseState = mutableStateOf(true)
    private var editable = false

    override suspend fun main() {
        val uuid = intent.uuid ?: return finish()
        val profile = withProfile { queryByUUID(uuid) } ?: return finish()
        editable = profile.type != Profile.Type.Url
        val client = FilesClient(this)
        val stack = Stack<String>()
        val root = uuid.toString()

        suspend fun refresh() {
            val id = stack.lastOrNull() ?: root
            val listed = withContext(Dispatchers.IO) { client.list(id) }
            val visible = if (stack.empty()) {
                val config = listed.firstOrNull { it.id.endsWith("config.yaml") }
                if (config == null || config.size > 0) listed else listOf(config)
            } else listed
            filesState.value = visible
            currentInBaseState.value = stack.empty()
        }
        refresh()
        setContent {
            AuroraTheme {
                FilesScreen(
                    files = filesState.value,
                    currentInBase = currentInBaseState.value,
                    configurationEditable = editable,
                    onBack = { if (stack.empty()) finish() else { stack.pop(); launch { refresh() } } },
                    onOpenDirectory = { file -> stack.push(file.id); launch { refresh() } },
                    onOpenFile = { file -> startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(client.buildDocumentUri(file.id), "text/plain").grantPermissions()) },
                    onImport = { file, uri, name -> launch {
                        try {
                            withContext(Dispatchers.IO) {
                                if (file == null) client.importDocument(stack.last(), uri, name ?: "File")
                                else client.copyDocument(file.id, uri)
                            }
                            refresh()
                        } catch (e: Exception) { showFailure(e) }
                    } },
                    onExport = { file, uri -> launch {
                        try { withContext(Dispatchers.IO) { client.copyDocument(uri, file.id) }; refresh() }
                        catch (e: Exception) { showFailure(e) }
                    } },
                    onRename = { file, name -> launch {
                        try { withContext(Dispatchers.IO) { client.renameDocument(file.id, name) }; refresh() }
                        catch (e: Exception) { showFailure(e) }
                    } },
                    onDelete = { file -> launch {
                        try { withContext(Dispatchers.IO) { client.deleteDocument(file.id) }; refresh() }
                        catch (e: Exception) { showFailure(e) }
                    } },
                )
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ActivityStop -> runCatching { refresh() }
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
