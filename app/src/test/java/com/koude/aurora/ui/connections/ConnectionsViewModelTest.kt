package com.koude.aurora.ui.connections

import com.github.kr328.clash.core.model.ConnectionInfo
import com.koude.aurora.data.connections.ConnectionsRepository
import com.koude.aurora.data.connections.ConnectionsSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val connection = ConnectionInfo(id = "one", host = "example.com")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun refreshLoadsConnectionsAndLabels() = runTest(dispatcher) {
        val repository = FakeConnectionsRepository()
        repository.snapshot = ConnectionsSnapshot(listOf(connection), mapOf("browser" to "Browser"))
        val viewModel = ConnectionsViewModel(repository)

        viewModel.refresh(true)
        assertTrue(viewModel.uiState.value.loading)
        advanceUntilIdle()

        assertEquals(listOf(connection), viewModel.uiState.value.connections)
        assertEquals(mapOf("browser" to "Browser"), viewModel.uiState.value.appLabels)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test fun serviceStopDiscardsLateConnectionResult() = runTest(dispatcher) {
        val pending = CompletableDeferred<ConnectionsSnapshot>()
        val repository = FakeConnectionsRepository()
        repository.queryAction = { withContext(NonCancellable) { pending.await() } }
        val viewModel = ConnectionsViewModel(repository)

        viewModel.refresh(true)
        advanceUntilIdle()
        viewModel.refresh(false)
        pending.complete(ConnectionsSnapshot(listOf(connection), emptyMap()))
        advanceUntilIdle()

        assertEquals(ConnectionsUiState(), viewModel.uiState.value)
    }

    @Test fun closingOnlyRequestedConnectionsRefreshesList() = runTest(dispatcher) {
        val repository = FakeConnectionsRepository()
        repository.snapshot = ConnectionsSnapshot(listOf(connection), emptyMap())
        val viewModel = ConnectionsViewModel(repository)
        viewModel.refresh(true)
        advanceUntilIdle()

        repository.snapshot = ConnectionsSnapshot(emptyList(), emptyMap())
        viewModel.close(listOf("one")) { throw AssertionError(it) }
        advanceUntilIdle()

        assertEquals(listOf(listOf("one")), repository.closedIds)
        assertEquals(emptyList<ConnectionInfo>(), viewModel.uiState.value.connections)
        assertEquals(2, repository.queryCount)
    }

    @Test fun closeFailureStillRefreshesAndReportsError() = runTest(dispatcher) {
        val repository = FakeConnectionsRepository()
        repository.closeError = IllegalStateException("close failed")
        val viewModel = ConnectionsViewModel(repository)
        viewModel.refresh(true)
        advanceUntilIdle()
        var reported: Throwable? = null

        viewModel.close(listOf("one")) { reported = it }
        advanceUntilIdle()

        assertEquals("close failed", reported?.message)
        assertEquals(2, repository.queryCount)
    }

    private class FakeConnectionsRepository : ConnectionsRepository {
        var snapshot = ConnectionsSnapshot(emptyList(), emptyMap())
        var queryAction: (suspend () -> ConnectionsSnapshot)? = null
        var closeError: Exception? = null
        var queryCount = 0
        val closedIds = mutableListOf<List<String>>()

        override suspend fun query(): ConnectionsSnapshot {
            queryCount++
            return queryAction?.invoke() ?: snapshot
        }

        override suspend fun close(ids: List<String>) {
            closedIds += ids
            closeError?.let { throw it }
        }
    }
}
