package com.koude.aurora.ui.profiles

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfileFilesRepository
import com.koude.aurora.data.profiles.ServiceProfileFilesRepository
import java.util.UUID

class ProfileFilesViewModel(private val repository: ProfileFilesRepository) : ViewModel() {
    private val directoryStack = ArrayDeque<String>()
    private lateinit var rootId: String

    var files by mutableStateOf<List<File>>(emptyList())
        private set
    var inBase by mutableStateOf(true)
        private set
    var editable by mutableStateOf(false)
        private set

    suspend fun load(uuid: UUID): Boolean {
        val profile = repository.queryProfile(uuid) ?: return false
        rootId = uuid.toString()
        directoryStack.clear()
        editable = profile.type != Profile.Type.Url
        refresh()
        return true
    }

    suspend fun refresh() {
        val listed = repository.list(directoryStack.lastOrNull() ?: rootId)
        files = if (directoryStack.isEmpty()) {
            val config = listed.firstOrNull { it.id.endsWith("config.yaml") }
            if (config == null || config.size > 0) listed else listOf(config)
        } else listed
        inBase = directoryStack.isEmpty()
    }

    suspend fun enter(documentId: String) {
        directoryStack.addLast(documentId)
        refresh()
    }

    suspend fun back() {
        if (directoryStack.isNotEmpty()) {
            directoryStack.removeLast()
            refresh()
        }
    }

    suspend fun import(file: File?, source: Uri, name: String?) {
        if (file == null) repository.import(directoryStack.lastOrNull() ?: rootId, source, name ?: "File")
        else repository.copyInto(file.id, source)
        refresh()
    }

    suspend fun export(file: File, target: Uri) {
        repository.export(file.id, target)
        refresh()
    }

    suspend fun rename(file: File, name: String) {
        repository.rename(file.id, name)
        refresh()
    }

    suspend fun delete(file: File) {
        repository.delete(file.id)
        refresh()
    }

    fun documentUri(file: File): Uri = repository.documentUri(file.id)

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileFilesViewModel(ServiceProfileFilesRepository(context.applicationContext)) as T
        }
    }
}
