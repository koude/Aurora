package com.koude.aurora.data.profiles

import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import java.util.UUID

interface ProfilePropertiesRepository {
    suspend fun query(uuid: UUID): Profile?
    suspend fun patch(profile: Profile)
    suspend fun commit(uuid: UUID, progress: (FetchStatus) -> Unit)
    suspend fun release(uuid: UUID)
}

class ServiceProfilePropertiesRepository : ProfilePropertiesRepository {
    override suspend fun query(uuid: UUID): Profile? = withProfile { queryByUUID(uuid) }

    override suspend fun patch(profile: Profile) {
        withProfile { patch(profile.uuid, profile.name, profile.source, profile.interval, profile.ageSecretKey) }
    }

    override suspend fun commit(uuid: UUID, progress: (FetchStatus) -> Unit) {
        withProfile { commit(uuid, progress) }
    }

    override suspend fun release(uuid: UUID) {
        withProfile { release(uuid) }
    }
}
