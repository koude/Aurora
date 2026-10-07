package com.koude.aurora.ui.proxy

import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.ProxySort
import com.koude.aurora.data.proxy.ProxyPreferences
import com.koude.aurora.data.proxy.ProxyRepository
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
class ProxyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val endpoint = Proxy("JP", "Japan", "", "Shadowsocks", 42, false)
    private val nested = Proxy("Automatic", "Automatic", "", "URLTest", 65535, true)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun refreshResolvesNestedEndpointWithoutTestingIt() = runTest(dispatcher) {
        val repository = FakeProxyRepository(
            linkedMapOf(
                "Main" to ProxyGroup("Selector", listOf(nested), "Automatic"),
                "Automatic" to ProxyGroup("URLTest", listOf(endpoint), "JP"),
            ),
        )
        val viewModel = ProxyViewModel(repository, FakePreferences())

        viewModel.refresh(true)
        advanceUntilIdle()

        val group = viewModel.uiState.value.groups.single()
        assertEquals(listOf("Automatic", "JP"), group.nestedRoutes["Automatic"]?.names)
        assertEquals(42, group.activeDelay)
        assertFalse(group.activeDelayTested)
        assertTrue(repository.tested.isEmpty())
    }

    @Test fun collapsedRefreshTestsOnlyActualEndpoint() = runTest(dispatcher) {
        val repository = FakeProxyRepository(
            linkedMapOf(
                "Main" to ProxyGroup("Selector", listOf(nested), "Automatic"),
                "Automatic" to ProxyGroup("URLTest", listOf(endpoint), "JP"),
            ),
        )
        val viewModel = ProxyViewModel(repository, FakePreferences())
        viewModel.refresh(true)
        advanceUntilIdle()

        viewModel.testActiveEndpoints()
        advanceUntilIdle()

        assertEquals(listOf("JP"), repository.tested)
        assertTrue(viewModel.uiState.value.groups.single().activeDelayTested)
    }

    @Test fun expansionIsSingleGroupAndClearedOnlyWhenLeavingPage() = runTest(dispatcher) {
        val repository = FakeProxyRepository(
            linkedMapOf("Main" to ProxyGroup("Selector", listOf(endpoint), "JP")),
        )
        val viewModel = ProxyViewModel(repository, FakePreferences())
        viewModel.refresh(true)
        advanceUntilIdle()

        viewModel.setGroupExpanded("Main", true)
        viewModel.setGroupExpanded("Other", true)
        assertEquals(mapOf("Other" to true), viewModel.uiState.value.expandedGroups)
        viewModel.onRouteChanged("proxy", "proxy")
        assertEquals(mapOf("Other" to true), viewModel.uiState.value.expandedGroups)
        viewModel.onRouteChanged("home", "proxy")
        assertTrue(viewModel.uiState.value.expandedGroups.isEmpty())
    }

    @Test fun expandedRefreshHealthChecksGroupInsteadOfEndpoint() = runTest(dispatcher) {
        val repository = FakeProxyRepository(
            linkedMapOf("Main" to ProxyGroup("Selector", listOf(endpoint), "JP")),
        )
        val viewModel = ProxyViewModel(repository, FakePreferences())
        viewModel.refresh(true)
        advanceUntilIdle()

        viewModel.testGroup(0)
        advanceUntilIdle()

        assertEquals(listOf("Main"), repository.checked)
        assertTrue(repository.tested.isEmpty())
        assertTrue(viewModel.uiState.value.groups.single().delayTested)
    }

    @Test fun selectingProxyUpdatesRepositoryAndDisplayedSelection() = runTest(dispatcher) {
        val second = Proxy("US", "United States", "", "Shadowsocks", 80, false)
        val repository = FakeProxyRepository(
            linkedMapOf("Main" to ProxyGroup("Selector", listOf(endpoint, second), "JP")),
        )
        val preferences = FakePreferences()
        val viewModel = ProxyViewModel(repository, preferences)
        viewModel.refresh(true)
        advanceUntilIdle()

        viewModel.selectGroup(0)
        viewModel.selectProxy(0, "US")
        advanceUntilIdle()

        assertEquals("Main", preferences.lastGroup)
        assertEquals("US", viewModel.uiState.value.groups.single().selectedProxy)
        assertEquals(null, viewModel.uiState.value.groups.single().selectingProxy)
    }

    @Test fun sortAndVisibilityChangesAreSavedAndRefreshed() = runTest(dispatcher) {
        val repository = FakeProxyRepository(
            linkedMapOf("Main" to ProxyGroup("Selector", listOf(endpoint), "JP")),
        )
        val preferences = FakePreferences()
        val viewModel = ProxyViewModel(repository, preferences)
        viewModel.refresh(true)
        advanceUntilIdle()

        viewModel.setSort(ProxySort.Delay)
        viewModel.setHideUnselectable(true)
        advanceUntilIdle()

        assertEquals(ProxySort.Delay, preferences.sort)
        assertTrue(preferences.hideUnselectable)
        assertEquals(ProxySort.Delay, viewModel.uiState.value.sort)
        assertTrue(viewModel.uiState.value.hideUnselectableGroups)
    }

    @Test fun serviceStopDiscardsLateResultAndClearsTestState() = runTest(dispatcher) {
        val pending = CompletableDeferred<List<String>>()
        val repository = FakeProxyRepository(
            linkedMapOf("Main" to ProxyGroup("Selector", listOf(endpoint), "JP")),
        )
        repository.namesAction = { withContext(NonCancellable) { pending.await() } }
        val viewModel = ProxyViewModel(repository, FakePreferences())

        viewModel.refresh(true)
        advanceUntilIdle()
        viewModel.onServiceStopped()
        pending.complete(listOf("Main"))
        advanceUntilIdle()

        assertEquals(ProxyUiState(serviceRunning = false), viewModel.uiState.value)
    }

    private class FakePreferences : ProxyPreferences {
        override var sort = ProxySort.Default
        override var hideUnselectable = false
        override var lastGroup = ""
    }

    private class FakeProxyRepository(
        private val groups: LinkedHashMap<String, ProxyGroup>,
    ) : ProxyRepository {
        var namesAction: (suspend () -> List<String>)? = null
        val tested = mutableListOf<String>()
        val checked = mutableListOf<String>()

        override suspend fun groupNames(hideUnselectable: Boolean): List<String> =
            namesAction?.invoke() ?: groups.keys.filter { it != "Automatic" }

        override suspend fun group(name: String, sort: ProxySort): ProxyGroup = groups.getValue(name)

        override suspend fun select(group: String, proxy: String) {
            groups[group] = groups.getValue(group).copy(now = proxy)
        }

        override suspend fun test(proxy: String) { tested += proxy }

        override suspend fun healthCheck(group: String) { checked += group }
    }
}
