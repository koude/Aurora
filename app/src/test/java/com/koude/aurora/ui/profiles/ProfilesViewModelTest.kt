package com.koude.aurora.ui.profiles

import com.koude.aurora.data.profiles.ProfileRepository
import com.koude.aurora.model.ProfileKind
import com.koude.aurora.model.ProfileSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val viewModel = ProfilesViewModel(repository)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(emptyList<ProfileSummary>(), viewModel.uiState.value.profiles)

        repository.profilesFlow.value = listOf(profile)
        advanceUntilIdle()
        assertEquals(listOf(profile), viewModel.uiState.value.profiles)
    }

    @Test fun forwardsProfileActions() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = ProfilesViewModel(repository)
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
        val viewModel = ProfilesViewModel(repository)
        advanceUntilIdle()

        viewModel.activate(profileId)
        advanceUntilIdle()

        assertEquals(listOf(profile), viewModel.uiState.value.profiles)
        assertEquals("activation failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.loading)
    }

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
    }
}
