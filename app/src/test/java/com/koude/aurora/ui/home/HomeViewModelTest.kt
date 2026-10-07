package com.koude.aurora.ui.home

import com.github.kr328.clash.core.model.TunnelState
import com.koude.aurora.data.home.HomeConnectionSnapshot
import com.koude.aurora.data.home.HomeRepository
import com.koude.aurora.data.home.HomeTrafficSnapshot
import com.koude.aurora.data.home.WebsiteLatencyRepository
import com.koude.aurora.model.WebsiteLatencySite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun connectionRefreshLoadsModeAndActiveProfile() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        repository.connectionResult = HomeConnectionSnapshot(TunnelState.Mode.Global, "Daily")
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)

        viewModel.refreshConnection(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.running)
        assertEquals(TunnelState.Mode.Global, viewModel.uiState.value.mode)
        assertEquals("Daily", viewModel.uiState.value.profileName)
    }

    @Test
    fun trafficRefreshShowsSpeedsAndStopDiscardsLateSample() = runTest(dispatcher) {
        val pending = CompletableDeferred<HomeTrafficSnapshot>()
        val repository = FakeHomeRepository()
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.refreshConnection(true)
        advanceUntilIdle()
        repository.trafficAction = { withContext(NonCancellable) { pending.await() } }

        viewModel.refreshTraffic()
        advanceUntilIdle()
        viewModel.onServiceStopped()
        pending.complete(HomeTrafficSnapshot("10 KB/s", "20 KB/s"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.running)
        assertEquals("-- B/s", viewModel.uiState.value.uploadSpeed)
        assertEquals("-- B/s", viewModel.uiState.value.downloadSpeed)
    }

    @Test
    fun serviceStopDiscardsLateConnectionResult() = runTest(dispatcher) {
        val pending = CompletableDeferred<HomeConnectionSnapshot>()
        val repository = FakeHomeRepository()
        repository.connectionAction = { withContext(NonCancellable) { pending.await() } }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)

        viewModel.refreshConnection(true)
        advanceUntilIdle()
        repository.connectionAction = null
        viewModel.onServiceStopped()
        pending.complete(HomeConnectionSnapshot(TunnelState.Mode.Global, "Stale"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.running)
        assertEquals(TunnelState.Mode.Rule, viewModel.uiState.value.mode)
        assertEquals(null, viewModel.uiState.value.profileName)
    }

    @Test
    fun trafficRefreshUsesRepositoryFormattedSpeeds() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        repository.trafficAction = { HomeTrafficSnapshot("10 KB/s", "20 KB/s") }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.refreshConnection(true)
        advanceUntilIdle()

        viewModel.refreshTraffic()
        advanceUntilIdle()

        assertEquals("10 KB/s", viewModel.uiState.value.uploadSpeed)
        assertEquals("20 KB/s", viewModel.uiState.value.downloadSpeed)
    }

    @Test
    fun modeChangesOnlyAfterRepositoryPatchSucceeds() = runTest(dispatcher) {
        val pending = CompletableDeferred<Unit>()
        val repository = FakeHomeRepository()
        repository.modeAction = { pending.await() }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)

        viewModel.setMode(TunnelState.Mode.Direct)
        runCurrent()
        assertEquals(TunnelState.Mode.Rule, viewModel.uiState.value.mode)

        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(TunnelState.Mode.Direct), repository.patchedModes)
        assertEquals(TunnelState.Mode.Direct, viewModel.uiState.value.mode)
    }

    @Test
    fun singleSiteTestUpdatesOnlyThatSite() = runTest(dispatcher) {
        val repository = FakeWebsiteLatencyRepository { 42L }
        val viewModel = HomeViewModel(repository)

        viewModel.testSiteLatency(WebsiteLatencySite.Apple)
        advanceUntilIdle()

        assertEquals("42 ms", viewModel.uiState.value.appleLatency)
        assertEquals("-- ms", viewModel.uiState.value.githubLatency)
        assertEquals(setOf(WebsiteLatencySite.Apple), repository.requestedSites)
        assertTrue(viewModel.uiState.value.testingLatencySites.isEmpty())
        assertFalse(viewModel.uiState.value.latencyTesting)
    }

    @Test
    fun testAllSitesReportsEachResultAndClearsLoadingState() = runTest(dispatcher) {
        val repository = FakeWebsiteLatencyRepository { site ->
            (site.ordinal + 10).toLong()
        }
        val viewModel = HomeViewModel(repository)

        viewModel.testAllSiteLatencies()
        advanceUntilIdle()

        assertEquals(WebsiteLatencySite.entries.toSet(), repository.requestedSites)
        assertEquals("10 ms", viewModel.uiState.value.appleLatency)
        assertEquals("11 ms", viewModel.uiState.value.githubLatency)
        assertEquals("12 ms", viewModel.uiState.value.youtubeLatency)
        assertEquals("13 ms", viewModel.uiState.value.googleLatency)
        assertTrue(viewModel.uiState.value.testingLatencySites.isEmpty())
        assertFalse(viewModel.uiState.value.latencyTesting)
    }

    @Test
    fun failedSiteTestShowsTimeout() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null })

        viewModel.testSiteLatency(WebsiteLatencySite.Google)
        advanceUntilIdle()

        assertEquals("超时", viewModel.uiState.value.googleLatency)
        assertTrue(viewModel.uiState.value.testingLatencySites.isEmpty())
    }

    @Test
    fun duplicateTapDoesNotStartSecondRequestForSameSite() = runTest(dispatcher) {
        val result = CompletableDeferred<Long?>()
        val repository = FakeWebsiteLatencyRepository { result.await() }
        val viewModel = HomeViewModel(repository)

        viewModel.testSiteLatency(WebsiteLatencySite.YouTube)
        viewModel.testSiteLatency(WebsiteLatencySite.YouTube)
        runCurrent()

        assertEquals(listOf(WebsiteLatencySite.YouTube), repository.requests)

        result.complete(25L)
        advanceUntilIdle()

        assertEquals("25 ms", viewModel.uiState.value.youtubeLatency)
    }

    @Test
    fun routeTestNormalizesTargetAndStartsOnlyWhenServiceIsRunning() {
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null })
        viewModel.openRouteTest()
        viewModel.changeRouteTestTarget(" https://github.com/example/path ")

        val target = viewModel.prepareRouteTest(
            serviceRunning = true,
            invalidTargetMessage = "invalid",
            serviceRequiredMessage = "offline",
        )

        assertEquals("github.com", target?.target)
        assertEquals("github.com", viewModel.routeTestState.value.target)
        assertTrue(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.errorMessage)
    }

    @Test
    fun routeTestReportsInvalidTargetAndRequiresRunningService() {
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null })
        viewModel.openRouteTest()
        viewModel.changeRouteTestTarget("not a target")

        assertEquals(
            null,
            viewModel.prepareRouteTest(true, "invalid", "offline"),
        )
        assertEquals("invalid", viewModel.routeTestState.value.errorMessage)
        assertFalse(viewModel.routeTestState.value.isTesting)

        viewModel.changeRouteTestTarget("example.com")
        assertEquals(
            null,
            viewModel.prepareRouteTest(false, "invalid", "offline"),
        )
        assertEquals("offline", viewModel.routeTestState.value.errorMessage)
        assertFalse(viewModel.routeTestState.value.isTesting)
    }

    @Test
    fun dismissedRouteTestCannotBeChangedByLateFailure() {
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null })
        viewModel.openRouteTest()
        val target = viewModel.prepareRouteTest(true, "invalid", "offline")
        assertEquals("github.com", target?.target)

        viewModel.dismissRouteTest()
        viewModel.failRouteTest(target!!, "late failure")

        assertFalse(viewModel.routeTestState.value.isOpen)
        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.errorMessage)
    }

    @Test
    fun lateResultFromPreviousRequestCannotOverwriteCurrentRequest() {
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null })
        viewModel.openRouteTest()
        val previous = viewModel.prepareRouteTest(true, "invalid", "offline")!!
        val current = viewModel.prepareRouteTest(true, "invalid", "offline")!!

        viewModel.failRouteTest(previous, "stale failure")

        assertTrue(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.errorMessage)

        viewModel.failRouteTest(current, "current failure")

        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals("current failure", viewModel.routeTestState.value.errorMessage)
    }

    private class FakeWebsiteLatencyRepository(
        private val result: suspend (WebsiteLatencySite) -> Long?,
    ) : WebsiteLatencyRepository {
        val requests = mutableListOf<WebsiteLatencySite>()
        val requestedSites: Set<WebsiteLatencySite>
            get() = requests.toSet()

        override suspend fun measure(site: WebsiteLatencySite): Long? {
            requests += site
            return result(site)
        }
    }

    private class FakeHomeRepository : HomeRepository {
        var connectionResult = HomeConnectionSnapshot(TunnelState.Mode.Rule, null)
        var connectionAction: (suspend () -> HomeConnectionSnapshot)? = null
        var trafficAction: (suspend () -> HomeTrafficSnapshot)? = null
        var modeAction: (suspend () -> Unit)? = null
        val patchedModes = mutableListOf<TunnelState.Mode>()

        override suspend fun connection(): HomeConnectionSnapshot =
            connectionAction?.invoke() ?: connectionResult

        override suspend fun traffic(): HomeTrafficSnapshot =
            trafficAction?.invoke() ?: HomeTrafficSnapshot("1 KB/s", "2 KB/s")

        override suspend fun setMode(mode: TunnelState.Mode) {
            modeAction?.invoke()
            patchedModes += mode
        }
    }
}
