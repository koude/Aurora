package com.koude.aurora.ui.settings

import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.service.model.AccessControlMode
import com.koude.aurora.data.settings.AccessControlOptions
import com.koude.aurora.data.settings.AccessControlRepository
import com.koude.aurora.data.settings.AccessControlSelection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessControlViewModelTest {
    @Test fun legacyAcceptAllBecomesEmptyDenyList() = runTest {
        val repository = FakeRepository(AccessControlSelection(AccessControlMode.AcceptAll, setOf("old")))
        val editor = AccessControlViewModel(repository)

        editor.load()

        assertEquals(AccessControlMode.DenySelected, editor.mode)
        assertTrue(editor.selectedPackages.isEmpty())
        assertFalse(editor.hasUnsavedChanges)
    }

    @Test fun applyStoresFirstAndRestartsOnlyWhenNeeded() = runTest {
        val repository = FakeRepository(AccessControlSelection(AccessControlMode.DenySelected, setOf("selected")))
        val editor = AccessControlViewModel(repository)
        editor.load()
        editor.changeMode(AccessControlMode.AcceptSelected)
        editor.selectNone()
        // An empty allow-list must not be applied.
        assertFalse(editor.beginApply())
        editor.changeMode(AccessControlMode.DenySelected)
        editor.import(emptySet())
        assertTrue(editor.hasUnsavedChanges)

        assertTrue(editor.beginApply())
        assertFalse(editor.beginApply())
        val message = editor.apply(true) { repository.operations += "restart" }

        assertEquals("已保存并应用", message)
        assertEquals(listOf("save", "restart"), repository.operations)
        assertFalse(editor.applying)
        assertFalse(editor.hasUnsavedChanges)
    }

    @Test fun restartFailureStillMarksSavedSelection() = runTest {
        val repository = FakeRepository(AccessControlSelection(AccessControlMode.DenySelected, setOf("selected")))
        val editor = AccessControlViewModel(repository)
        editor.load()
        editor.changeMode(AccessControlMode.AcceptSelected)
        assertTrue(editor.hasUnsavedChanges)
        assertTrue(editor.beginApply())

        val message = editor.apply(true) { error("VPN restart failed") }

        assertEquals("名单已保存，代理重启失败，请手动重新连接", message)
        assertFalse(editor.applying)
        assertFalse(editor.hasUnsavedChanges)
    }

    private class FakeRepository(private val initial: AccessControlSelection) : AccessControlRepository {
        val operations = mutableListOf<String>()
        override suspend fun selection(): AccessControlSelection = initial
        override fun options() = AccessControlOptions(false, AppInfoSort.Label, false, true)
        override fun setShowSystemApps(value: Boolean) = Unit
        override fun setSort(value: AppInfoSort) = Unit
        override fun setReverse(value: Boolean) = Unit
        override suspend fun apps(selected: Set<String>): List<AppInfo> = emptyList()
        override suspend fun save(requested: AccessControlSelection): Boolean {
            operations += "save"
            return true
        }
    }
}
