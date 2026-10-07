package com.koude.aurora.ui.profiles

import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfileCreationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class NewProfileViewModelTest {
    @Test fun sourceTypesArePreserved() = runTest {
        val repository = FakeRepository()
        val creator = NewProfileViewModel(repository)

        creator.createFile("Local")
        creator.createUrl("Subscription")
        creator.createUrl("QR", "https://example.com")
        creator.createExternal("External", "content://profile")

        assertEquals(
            listOf(
                Triple(Profile.Type.File, "Local", null),
                Triple(Profile.Type.Url, "Subscription", null),
                Triple(Profile.Type.Url, "QR", "https://example.com"),
                Triple(Profile.Type.External, "External", "content://profile"),
            ),
            repository.requests,
        )
    }

    private class FakeRepository : ProfileCreationRepository {
        val requests = mutableListOf<Triple<Profile.Type, String, String?>>()
        override suspend fun create(type: Profile.Type, name: String, source: String?): UUID {
            requests += Triple(type, name, source)
            return UUID.randomUUID()
        }
    }
}
