package com.koude.aurora.ui.providers

import com.github.kr328.clash.core.model.Provider
import com.koude.aurora.data.providers.ProvidersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProvidersViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val remote = Provider("Remote", Provider.Type.Proxy, Provider.VehicleType.HTTP, 100L)
    private val inline = Provider("Inline", Provider.Type.Rule, Provider.VehicleType.Inline, 50L)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun refreshLoadsProviderRows() = runTest(dispatcher) {
        val repository = FakeRepository(listOf(remote, inline))
        val viewModel = ProvidersViewModel(repository)

        viewModel.refresh()

        assertEquals(listOf(remote, inline), viewModel.rows.value.map { it.provider })
        assertEquals(100L, viewModel.rows.value.first().updatedAt)
    }

    @Test fun updateSetsTimestampAndIgnoresInlineProviders() = runTest(dispatcher) {
        val repository = FakeRepository(listOf(remote, inline))
        val viewModel = ProvidersViewModel(repository) { 200L }
        viewModel.refresh()

        viewModel.update(0)
        assertTrue(viewModel.rows.value.first().updating)
        viewModel.update(1)
        advanceUntilIdle()

        assertEquals(listOf(Provider.Type.Proxy to "Remote"), repository.updates)
        assertFalse(viewModel.rows.value.first().updating)
        assertEquals(200L, viewModel.rows.value.first().updatedAt)
    }

    @Test fun failedUpdateRestoresButtonAndReportsProvider() = runTest(dispatcher) {
        val repository = FakeRepository(listOf(remote)).apply { updateError = IllegalStateException("offline") }
        val viewModel = ProvidersViewModel(repository)
        viewModel.refresh()

        viewModel.update(0)
        advanceUntilIdle()

        assertFalse(viewModel.rows.value.first().updating)
        assertEquals(100L, viewModel.rows.value.first().updatedAt)
        assertEquals(ProviderUpdateError("Remote", "offline"), viewModel.errors.first())
    }

    private class FakeRepository(private val providers: List<Provider>) : ProvidersRepository {
        val updates = mutableListOf<Pair<Provider.Type, String>>()
        var updateError: Exception? = null

        override suspend fun query() = providers
        override suspend fun update(type: Provider.Type, name: String) {
            updates += type to name
            updateError?.let { throw it }
        }
    }
}
