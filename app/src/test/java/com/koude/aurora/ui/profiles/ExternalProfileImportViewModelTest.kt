package com.koude.aurora.ui.profiles

import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ExternalProfileImportRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class ExternalProfileImportViewModelTest {
    @Test fun fileLinkPreservesSourceAndClampsUpdateInterval() = runTest {
        val repository = FakeRepository()
        val importer = ExternalProfileImportViewModel(repository)

        importer.importProfile("FILE", "Imported", "content://config", "5")

        assertEquals(Profile.Type.File, repository.type)
        assertEquals("Imported", repository.name)
        assertEquals("content://config", repository.source)
        assertEquals(15 * 60_000L, repository.intervalMillis)
    }

    @Test fun missingTypeAndIntervalUseUrlWithoutScheduledUpdates() = runTest {
        val repository = FakeRepository()
        val importer = ExternalProfileImportViewModel(repository)

        importer.importProfile(null, "Subscription", "https://example.com", null)

        assertEquals(Profile.Type.Url, repository.type)
        assertEquals(0L, repository.intervalMillis)
    }

    private class FakeRepository : ExternalProfileImportRepository {
        var type: Profile.Type? = null
        var name: String? = null
        var source: String? = null
        var intervalMillis: Long? = null
        override suspend fun createAndPatch(type: Profile.Type, name: String, source: String, intervalMillis: Long): UUID {
            this.type = type
            this.name = name
            this.source = source
            this.intervalMillis = intervalMillis
            return UUID.randomUUID()
        }
    }
}
