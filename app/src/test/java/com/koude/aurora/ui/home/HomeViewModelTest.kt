package com.koude.aurora.ui.home

import com.koude.aurora.data.home.WebsiteLatencyRepository
import com.koude.aurora.model.WebsiteLatencySite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
}
