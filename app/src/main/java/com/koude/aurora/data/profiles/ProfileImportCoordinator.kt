package com.koude.aurora.data.profiles

import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.UUID

interface ProfileImportGateway {
    suspend fun create(type: Profile.Type, name: String, source: String): UUID
    suspend fun commit(id: UUID, onProgress: (FetchStatus) -> Unit)
    suspend fun delete(id: UUID)
}

class ServiceProfileImportGateway : ProfileImportGateway {
    override suspend fun create(type: Profile.Type, name: String, source: String): UUID =
        withProfile { create(type, name, source) }

    override suspend fun commit(id: UUID, onProgress: (FetchStatus) -> Unit) {
        withProfile { commit(id) { status -> onProgress(status) } }
    }

    override suspend fun delete(id: UUID) {
        withProfile { delete(id) }
    }
}

class ProfileImportCoordinator(private val gateway: ProfileImportGateway) {
    private val mutex = Mutex()

    /** Returns false when another import is active; failures are rethrown after cleanup. */
    suspend fun import(
        type: Profile.Type,
        name: String,
        source: String,
        prepare: suspend (UUID) -> Unit,
        onProgress: (FetchStatus) -> Unit,
    ): Boolean {
        if (!mutex.tryLock()) return false
        var id: UUID? = null
        try {
            id = gateway.create(type, name, source)
            prepare(id)
            gateway.commit(id, onProgress)
            return true
        } catch (error: Throwable) {
            id?.let { createdId ->
                withContext(NonCancellable) {
                    runCatching { gateway.delete(createdId) }
                }
            }
            throw error
        } finally {
            mutex.unlock()
        }
    }
}
