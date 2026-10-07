package com.koude.aurora.ui.profiles

import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfilePropertiesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProfilePropertiesViewModelTest {
    private val uuid = UUID.randomUUID()
    private val original = Profile(
        uuid, "Original", Profile.Type.Url, "https://example.com", false,
        0, 0, 0, 0, 0, 0, false, false,
    )

    @Test fun stopAutoSavesOnlyChangedProfile() = runTest {
        val repository = FakeRepository(original)
        val editor = ProfilePropertiesViewModel(repository)
        assertTrue(editor.load(uuid))
        editor.autoSave()
        assertTrue(repository.patches.isEmpty())

        editor.update(original.copy(name = "Changed"))
        assertTrue(editor.hasUnsavedChanges)
        editor.autoSave()

        assertEquals(listOf("Changed"), repository.patches.map { it.name })
    }

    @Test fun savePatchesAndCommitsBeforeRelease() = runTest {
        val repository = FakeRepository(original)
        val editor = ProfilePropertiesViewModel(repository)
        editor.load(uuid)
        editor.update(original.copy(name = "Changed"))

        editor.save()
        assertEquals(listOf("patch", "commit"), repository.operations)
        assertFalse(editor.saving)
        editor.release(uuid)
        editor.autoSave()
        assertEquals(listOf("patch", "commit", "release"), repository.operations)
    }

    private class FakeRepository(private val source: Profile) : ProfilePropertiesRepository {
        val patches = mutableListOf<Profile>()
        val operations = mutableListOf<String>()
        override suspend fun query(uuid: UUID): Profile? = source.takeIf { it.uuid == uuid }
        override suspend fun patch(profile: Profile) {
            patches += profile
            operations += "patch"
        }
        override suspend fun commit(uuid: UUID, progress: (FetchStatus) -> Unit) {
            operations += "commit"
        }
        override suspend fun release(uuid: UUID) {
            operations += "release"
        }
    }
}
