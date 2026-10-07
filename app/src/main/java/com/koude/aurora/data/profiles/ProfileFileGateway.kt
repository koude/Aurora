package com.koude.aurora.data.profiles

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.remote.FilesClient
import java.util.UUID

interface ProfileFileGateway {
    suspend fun copyConfiguration(id: UUID, sourceUri: String)
}

class ServiceProfileFileGateway(context: Context) : ProfileFileGateway {
    private val files = FilesClient(context.applicationContext)

    override suspend fun copyConfiguration(id: UUID, sourceUri: String) {
        files.copyDocument("$id/config.yaml", Uri.parse(sourceUri))
    }
}
