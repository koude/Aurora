package com.koude.aurora.data.profiles

import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProfileImportCoordinatorTest {
    private val id = UUID.randomUUID()

    @Test fun successfulImportPreparesThenCommits() = runTest {
        val gateway = FakeGateway(id)
        val coordinator = ProfileImportCoordinator(gateway)
        val progress = mutableListOf<FetchStatus>()

        assertTrue(coordinator.import(Profile.Type.Url, "Test", "https://example.com", {
            gateway.events += "prepare"
        }, progress::add))

        assertEquals(listOf("create", "prepare", "commit"), gateway.events)
        assertEquals(listOf(gateway.status), progress)
    }

    @Test fun failedCommitDeletesNewProfile() = runTest {
        val gateway = FakeGateway(id).apply { failCommit = true }
        val coordinator = ProfileImportCoordinator(gateway)

        val failure = runCatching {
            coordinator.import(Profile.Type.Url, "Test", "https://example.com", {}, {})
        }.exceptionOrNull()

        assertEquals("commit failed", failure?.message)
        assertEquals(listOf("create", "commit", "delete"), gateway.events)
    }

    @Test fun failedPreparationDeletesNewProfile() = runTest {
        val gateway = FakeGateway(id)
        val coordinator = ProfileImportCoordinator(gateway)

        val failure = runCatching {
            coordinator.import(Profile.Type.File, "Test", "", { error("copy failed") }, {})
        }.exceptionOrNull()

        assertEquals("copy failed", failure?.message)
        assertEquals(listOf("create", "delete"), gateway.events)
    }

    @Test fun concurrentImportIsRejectedWithoutCreatingAnotherProfile() = runTest {
        val gateway = FakeGateway(id)
        val coordinator = ProfileImportCoordinator(gateway)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = async {
            coordinator.import(Profile.Type.File, "First", "", {
                entered.complete(Unit)
                release.await()
            }, {})
        }
        entered.await()

        assertFalse(coordinator.import(Profile.Type.File, "Second", "", {}, {}))
        release.complete(Unit)
        assertTrue(first.await())
        assertEquals(1, gateway.events.count { it == "create" })
    }

    private class FakeGateway(private val id: UUID) : ProfileImportGateway {
        val events = mutableListOf<String>()
        var failCommit = false
        val status = FetchStatus(FetchStatus.Action.FetchConfiguration, emptyList(), 0, 0)

        override suspend fun create(type: Profile.Type, name: String, source: String): UUID {
            events += "create"
            return id
        }

        override suspend fun commit(id: UUID, onProgress: (FetchStatus) -> Unit) {
            events += "commit"
            if (failCommit) error("commit failed")
            onProgress(status)
        }

        override suspend fun delete(id: UUID) {
            events += "delete"
        }
    }
}
