package com.koude.aurora.ui.profiles

import android.net.Uri
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.service.model.Profile
import com.koude.aurora.data.profiles.ProfileFilesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProfileFilesViewModelTest {
    private val uuid = UUID.randomUUID()
    private val profile = Profile(
        uuid, "Local", Profile.Type.File, "", false,
        0, 0, 0, 0, 0, 0, false, false,
    )

    @Test fun directoryNavigationKeepsRootAndVisibilityRules() = runTest {
        val root = uuid.toString()
        val emptyConfig = File("$root/config.yaml", "config.yaml", 0, 0, false)
        val folder = File("$root/assets", "assets", 0, 0, true)
        val child = File("$root/assets/a.txt", "a.txt", 10, 0, false)
        val repository = FakeRepository(profile, mapOf(root to listOf(emptyConfig, folder), folder.id to listOf(child)))
        val browser = ProfileFilesViewModel(repository)

        assertTrue(browser.load(uuid))
        assertTrue(browser.editable)
        assertTrue(browser.inBase)
        assertEquals(listOf(emptyConfig), browser.files)

        browser.enter(folder.id)
        assertFalse(browser.inBase)
        assertEquals(listOf(child), browser.files)

        browser.back()
        assertTrue(browser.inBase)
        assertEquals(listOf(emptyConfig), browser.files)
    }

    private class FakeRepository(
        private val profile: Profile,
        private val listings: Map<String, List<File>>,
    ) : ProfileFilesRepository {
        override suspend fun queryProfile(uuid: UUID): Profile? = profile.takeIf { it.uuid == uuid }
        override suspend fun list(parentId: String): List<File> = listings[parentId].orEmpty()
        override suspend fun import(parentId: String, source: Uri, name: String) = Unit
        override suspend fun copyInto(documentId: String, source: Uri) = Unit
        override suspend fun export(documentId: String, target: Uri) = Unit
        override suspend fun rename(documentId: String, name: String) = Unit
        override suspend fun delete(documentId: String) = Unit
        override fun documentUri(documentId: String): Uri = error("Not used")
    }
}
