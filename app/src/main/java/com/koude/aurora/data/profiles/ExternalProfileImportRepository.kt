package com.koude.aurora.data.profiles

import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import java.util.UUID

interface ExternalProfileImportRepository {
    suspend fun createAndPatch(type: Profile.Type, name: String, source: String, intervalMillis: Long): UUID
}

class ServiceExternalProfileImportRepository : ExternalProfileImportRepository {
    override suspend fun createAndPatch(type: Profile.Type, name: String, source: String, intervalMillis: Long): UUID =
        withProfile {
            create(type, name).also { patch(it, name, source, intervalMillis, null) }
        }
}
