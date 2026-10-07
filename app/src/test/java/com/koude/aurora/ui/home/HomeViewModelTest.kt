package com.koude.aurora.ui.home

import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.model.RoutePreview
import com.koude.aurora.data.home.HomeConnectionSnapshot
import com.koude.aurora.data.home.HomeRepository
import com.koude.aurora.data.home.HomeTrafficSnapshot
import com.koude.aurora.data.home.WebsiteLatencyRepository
import com.koude.aurora.model.WebsiteLatencySite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
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
    fun connectionStartRequiresAnImportedActiveProfile() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)

        assertEquals(false, viewModel.canStartConnection())
        repository.activeProfileReady = true
        assertEquals(true, viewModel.canStartConnection())
    }

    @Test
    fun connectionStartCheckFailureReportsErrorInsteadOfStarting() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        repository.activeProfileError = IllegalStateException("profile service unavailable")
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)

        assertEquals(null, viewModel.canStartConnection())
        assertEquals("profile service unavailable", viewModel.errors.first().message)
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
    fun routeTestNormalizesTargetAndQueriesRepository() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.openRouteTest()
        viewModel.changeRouteTestTarget(" https://github.com/example/path ")

        viewModel.submitRouteTest(
            serviceRunning = true,
            invalidTargetMessage = "invalid",
            serviceRequiredMessage = "offline",
            testFailedMessage = "failed",
        )
        advanceUntilIdle()

        assertEquals(listOf("github.com"), repository.previewTargets)
        assertEquals("github.com", viewModel.routeTestState.value.target)
        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.errorMessage)
        assertEquals("github.com", viewModel.routeTestState.value.preview?.target)
    }

    @Test
    fun routeTestReportsInvalidTargetAndRequiresRunningService() {
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null })
        viewModel.openRouteTest()
        viewModel.changeRouteTestTarget("not a target")

        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        assertEquals("invalid", viewModel.routeTestState.value.errorMessage)
        assertFalse(viewModel.routeTestState.value.isTesting)

        viewModel.changeRouteTestTarget("example.com")
        viewModel.submitRouteTest(false, "invalid", "offline", "failed")
        assertEquals("offline", viewModel.routeTestState.value.errorMessage)
        assertFalse(viewModel.routeTestState.value.isTesting)
    }

    @Test
    fun dismissedRouteTestCannotBeChangedByLateResult() = runTest(dispatcher) {
        val pending = CompletableDeferred<RoutePreview>()
        val repository = FakeHomeRepository()
        repository.previewAction = { withContext(NonCancellable) { pending.await() } }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.openRouteTest()
        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        runCurrent()

        viewModel.dismissRouteTest()
        pending.complete(preview("github.com"))
        advanceUntilIdle()

        assertFalse(viewModel.routeTestState.value.isOpen)
        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.preview)
    }

    @Test
    fun lateResultFromPreviousRequestCannotOverwriteCurrentRequest() = runTest(dispatcher) {
        val pending = CompletableDeferred<RoutePreview>()
        val repository = FakeHomeRepository()
        repository.previewAction = { target ->
            if (target == "github.com") withContext(NonCancellable) { pending.await() }
            else preview(target)
        }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.openRouteTest()
        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        runCurrent()
        viewModel.changeRouteTestTarget("example.com")
        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        advanceUntilIdle()

        pending.complete(preview("github.com"))
        advanceUntilIdle()

        assertEquals("example.com", viewModel.routeTestState.value.preview?.target)
        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.errorMessage)
    }

    @Test
    fun routePreviewErrorIsShownWithoutAResultCard() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        repository.previewAction = { preview(it).copy(error = "no matching rule") }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.openRouteTest()
        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        advanceUntilIdle()

        assertEquals("no matching rule", viewModel.routeTestState.value.errorMessage)
        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.preview)
    }

    @Test
    fun routePreviewExceptionShowsFailureMessage() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        repository.previewAction = { throw IllegalStateException("service unavailable") }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.openRouteTest()

        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        advanceUntilIdle()

        assertEquals("failed", viewModel.routeTestState.value.errorMessage)
        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.preview)
    }

    @Test
    fun serviceStopCancelsPendingRoutePreview() = runTest(dispatcher) {
        val pending = CompletableDeferred<RoutePreview>()
        val repository = FakeHomeRepository()
        repository.previewAction = { withContext(NonCancellable) { pending.await() } }
        val viewModel = HomeViewModel(FakeWebsiteLatencyRepository { null }, repository)
        viewModel.openRouteTest()
        viewModel.submitRouteTest(true, "invalid", "offline", "failed")
        runCurrent()

        viewModel.onServiceStopped()
        pending.complete(preview("github.com"))
        advanceUntilIdle()

        assertFalse(viewModel.routeTestState.value.isTesting)
        assertEquals(null, viewModel.routeTestState.value.preview)
    }

    private fun preview(target: String) = RoutePreview(
        target = target,
        mode = "rule",
        rule = "MATCH",
        policy = "Proxy",
        outbound = "JP",
    )

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
        var activeProfileReady = false
        var activeProfileError: Exception? = null
        var previewAction: (suspend (String) -> RoutePreview)? = null
        val previewTargets = mutableListOf<String>()
        val patchedModes = mutableListOf<TunnelState.Mode>()

        override suspend fun connection(): HomeConnectionSnapshot =
            connectionAction?.invoke() ?: connectionResult

        override suspend fun traffic(): HomeTrafficSnapshot =
            trafficAction?.invoke() ?: HomeTrafficSnapshot("1 KB/s", "2 KB/s")

        override suspend fun setMode(mode: TunnelState.Mode) {
            modeAction?.invoke()
            patchedModes += mode
        }

        override suspend fun previewRoute(target: String): RoutePreview {
            previewTargets += target
            return previewAction?.invoke(target) ?: RoutePreview(target, "rule", "MATCH", "Proxy", "JP")
        }

        override suspend fun hasActiveImportedProfile(): Boolean {
            activeProfileError?.let { throw it }
            return activeProfileReady
        }
    }
}
