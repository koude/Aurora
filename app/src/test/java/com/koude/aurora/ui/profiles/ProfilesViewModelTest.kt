package com.koude.aurora.ui.profiles

import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfileFileGateway
import com.koude.aurora.data.profiles.ProfileImportCoordinator
import com.koude.aurora.data.profiles.ProfileImportGateway
import com.koude.aurora.data.profiles.ProfileRepository
import com.koude.aurora.model.ProfileKind
import com.koude.aurora.model.ProfileSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val profileId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val profile = ProfileSummary(profileId, "Daily", ProfileKind.Url, true, true, 123L)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun loadsAndObservesProfiles() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = newViewModel(repository)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(emptyList<ProfileSummary>(), viewModel.uiState.value.profiles)

        repository.profilesFlow.value = listOf(profile)
        advanceUntilIdle()
        assertEquals(listOf(profile), viewModel.uiState.value.profiles)
    }

    @Test fun forwardsProfileActions() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = newViewModel(repository)
        advanceUntilIdle()

        viewModel.activate(profileId)
        viewModel.refresh()
        viewModel.update(profileId)
        viewModel.updateAll()
        viewModel.delete(profileId)
        advanceUntilIdle()

        assertEquals(listOf("activate:$profileId", "refresh", "update:$profileId", "updateAll", "delete:$profileId"), repository.actions)
    }

    @Test fun actionFailureRetainsLoadedProfilesAndReportsError() = runTest(dispatcher) {
        val repository = FakeProfileRepository(listOf(profile))
        repository.failActivate = true
        val viewModel = newViewModel(repository)
        advanceUntilIdle()

        viewModel.activate(profileId)
        advanceUntilIdle()

        assertEquals(listOf(profile), viewModel.uiState.value.profiles)
        assertEquals("activation failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test fun urlImportRunsOnceAndClearsProgress() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val gateway = FakeImportGateway(profileId)
        val viewModel = newViewModel(repository, gateway)

        viewModel.importUrl("Daily", "https://example.com")
        viewModel.importUrl("Duplicate", "https://example.org")
        assertEquals("正在连接订阅并准备下载…", viewModel.importProgress.value?.stage)
        advanceUntilIdle()

        assertEquals(listOf("create:Daily", "commit"), gateway.events)
        assertEquals(listOf("refresh"), repository.actions)
        assertEquals(null, viewModel.importProgress.value)
    }

    @Test fun fileImportCopiesConfigurationBeforeCommit() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val gateway = FakeImportGateway(profileId)
        val copied = mutableListOf<String>()
        val viewModel = newViewModel(repository, gateway, object : ProfileFileGateway {
            override suspend fun copyConfiguration(id: UUID, sourceUri: String) {
                copied += "$id:$sourceUri"
                gateway.events += "copy"
            }
        })

        viewModel.importFile("Local", "content://local/config.yaml")
        advanceUntilIdle()

        assertEquals(listOf("create:Local", "copy", "commit"), gateway.events)
        assertEquals(listOf("$profileId:content://local/config.yaml"), copied)
        assertEquals(null, viewModel.importProgress.value)
    }

    @Test fun importFailureReportsErrorAndClearsProgress() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val gateway = FakeImportGateway(profileId).apply { failCommit = true }
        val viewModel = newViewModel(repository, gateway)

        viewModel.importUrl("Daily", "https://example.com")
        advanceUntilIdle()

        assertEquals("订阅获取失败", viewModel.errors.first().title)
        assertEquals(listOf("create:Daily", "commit", "delete"), gateway.events)
        assertEquals(null, viewModel.importProgress.value)
    }

    @Test fun duplicateReturnsNewProfileForEditor() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = newViewModel(repository)

        viewModel.duplicate(profileId)
        advanceUntilIdle()

        assertEquals(profileId, viewModel.duplicatedProfiles.first())
        assertEquals(listOf("duplicate:$profileId", "refresh"), repository.actions)
    }

    private fun newViewModel(
        repository: FakeProfileRepository,
        gateway: FakeImportGateway = FakeImportGateway(profileId),
        fileGateway: ProfileFileGateway = object : ProfileFileGateway {
            override suspend fun copyConfiguration(id: UUID, sourceUri: String) = Unit
        },
    ) = ProfilesViewModel(repository, ProfileImportCoordinator(gateway), fileGateway)

    private class FakeProfileRepository(initial: List<ProfileSummary> = emptyList()) : ProfileRepository {
        val profilesFlow = MutableStateFlow(initial)
        override val profiles = profilesFlow
        val actions = mutableListOf<String>()
        var failActivate = false

        override suspend fun refresh() { actions += "refresh" }
        override suspend fun activate(id: UUID) {
            actions += "activate:$id"
            if (failActivate) error("activation failed")
        }
        override suspend fun update(id: UUID) { actions += "update:$id" }
        override suspend fun updateAll() { actions += "updateAll" }
        override suspend fun delete(id: UUID) { actions += "delete:$id" }
        override suspend fun duplicate(id: UUID): UUID {
            actions += "duplicate:$id"
            return id
        }
    }

    private class FakeImportGateway(private val id: UUID) : ProfileImportGateway {
        val events = mutableListOf<String>()
        var failCommit = false

        override suspend fun create(type: Profile.Type, name: String, source: String): UUID {
            events += "create:$name"
            return id
        }

        override suspend fun commit(id: UUID, onProgress: (FetchStatus) -> Unit) {
            events += "commit"
            if (failCommit) error("commit failed")
            onProgress(FetchStatus(FetchStatus.Action.FetchConfiguration, emptyList(), 0, 0))
        }

        override suspend fun delete(id: UUID) { events += "delete" }
    }
}
