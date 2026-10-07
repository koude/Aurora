package com.koude.aurora.data.profiles

import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import java.util.UUID

interface ProfileCreationRepository {
    suspend fun create(type: Profile.Type, name: String, source: String? = null): UUID
}

class ServiceProfileCreationRepository : ProfileCreationRepository {
    override suspend fun create(type: Profile.Type, name: String, source: String?): UUID = withProfile {
        if (source == null) create(type, name) else create(type, name, source)
    }
}
