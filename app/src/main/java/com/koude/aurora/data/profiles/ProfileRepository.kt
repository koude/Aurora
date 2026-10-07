package com.koude.aurora.data.profiles

import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.koude.aurora.model.ProfileKind
import com.koude.aurora.model.ProfileSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import java.util.UUID

interface ProfileRepository {
    val profiles: Flow<List<ProfileSummary>>

    suspend fun refresh()
    suspend fun activate(id: UUID)
    suspend fun update(id: UUID)
    suspend fun updateAll()
    suspend fun delete(id: UUID)
    suspend fun duplicate(id: UUID): UUID
}

class ServiceProfileRepository : ProfileRepository {
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val changes = callbackFlow {
        val observer = object : Broadcasts.Observer {
            override fun onProfileChanged() {
                trySend(Unit)
            }

            override fun onServiceRecreated() {
                trySend(Unit)
            }

            override fun onProfileUpdateCompleted(uuid: UUID?) {
                trySend(Unit)
            }

            override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
                trySend(Unit)
            }

            override fun onProfileLoaded() = Unit
            override fun onStarted() = Unit
            override fun onStopped(cause: String?) = Unit
        }

        Remote.broadcasts.addObserver(observer)
        awaitClose { Remote.broadcasts.removeObserver(observer) }
    }

    override val profiles: Flow<List<ProfileSummary>> = merge(changes, refreshRequests)
        .onStart { emit(Unit) }
        .map { withProfile { queryAll().map(Profile::toSummary) } }
        .flowOn(Dispatchers.IO)

    override suspend fun refresh() {
        refreshRequests.emit(Unit)
    }

    override suspend fun activate(id: UUID) {
        withProfile {
            queryByUUID(id)?.takeIf(Profile::imported)?.let { setActive(it) }
        }
    }

    override suspend fun update(id: UUID) {
        withProfile { update(id) }
    }

    override suspend fun updateAll() {
        withProfile {
            queryAll()
                .filter { it.imported && it.type != Profile.Type.File }
                .forEach { update(it.uuid) }
        }
    }

    override suspend fun delete(id: UUID) {
        withProfile { delete(id) }
    }

    override suspend fun duplicate(id: UUID): UUID = withProfile { clone(id) }
}

object ProfileRepositoryProvider {
    val instance: ProfileRepository by lazy(::ServiceProfileRepository)
}

private fun Profile.toSummary() = ProfileSummary(
    id = uuid,
    name = name,
    kind = when (type) {
        Profile.Type.File -> ProfileKind.File
        Profile.Type.Url -> ProfileKind.Url
        Profile.Type.External -> ProfileKind.External
    },
    active = active,
    imported = imported,
    updatedAt = updatedAt,
)
