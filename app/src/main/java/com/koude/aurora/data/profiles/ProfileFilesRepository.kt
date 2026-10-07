package com.koude.aurora.data.profiles

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import java.util.UUID

interface ProfileFilesRepository {
    suspend fun queryProfile(uuid: UUID): Profile?
    suspend fun list(parentId: String): List<File>
    suspend fun import(parentId: String, source: Uri, name: String)
    suspend fun copyInto(documentId: String, source: Uri)
    suspend fun export(documentId: String, target: Uri)
    suspend fun rename(documentId: String, name: String)
    suspend fun delete(documentId: String)
    fun documentUri(documentId: String): Uri
}

class ServiceProfileFilesRepository(context: Context) : ProfileFilesRepository {
    private val client = FilesClient(context.applicationContext)

    override suspend fun queryProfile(uuid: UUID): Profile? = withProfile { queryByUUID(uuid) }
    override suspend fun list(parentId: String): List<File> = client.list(parentId)
    override suspend fun import(parentId: String, source: Uri, name: String) = client.importDocument(parentId, source, name)
    override suspend fun copyInto(documentId: String, source: Uri) = client.copyDocument(documentId, source)
    override suspend fun export(documentId: String, target: Uri) = client.copyDocument(target, documentId)
    override suspend fun rename(documentId: String, name: String) {
        client.renameDocument(documentId, name)
    }
    override suspend fun delete(documentId: String) {
        client.deleteDocument(documentId)
    }
    override fun documentUri(documentId: String): Uri = client.buildDocumentUri(documentId)
}
