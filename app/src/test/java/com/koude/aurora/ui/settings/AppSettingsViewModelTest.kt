package com.koude.aurora.ui.settings

import com.github.kr328.clash.design.model.DarkMode
import com.koude.aurora.data.settings.AppSettingsRepository
import com.koude.aurora.data.settings.AppSettingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsViewModelTest {
    @Test fun refreshReadsStoredValuesAndRunningState() {
        val repository = FakeRepository()
        repository.snapshot = repository.snapshot.copy(
            autoRestart = true, darkMode = DarkMode.ForceDark, showTraffic = false,
        )
        val viewModel = AppSettingsViewModel(repository)

        viewModel.refresh(true)

        assertTrue(viewModel.uiState.value.autoRestart)
        assertEquals(DarkMode.ForceDark, viewModel.uiState.value.darkMode)
        assertFalse(viewModel.uiState.value.showTraffic)
        assertTrue(viewModel.uiState.value.running)
    }

    @Test fun settingChangesWriteThroughAndUpdateUi() {
        val repository = FakeRepository()
        val viewModel = AppSettingsViewModel(repository)
        viewModel.refresh(false)

        viewModel.setAutoRestart(true)
        viewModel.setDarkMode(DarkMode.ForceDark)
        viewModel.setHideAppIcon(true)
        viewModel.setHideFromRecents(true)
        viewModel.setShowTraffic(false)

        assertEquals(listOf("auto:true", "dark:ForceDark", "icon:true", "recents:true", "traffic:false"), repository.writes)
        assertTrue(viewModel.uiState.value.hideAppIcon)
        assertTrue(viewModel.uiState.value.hideFromRecents)
        assertFalse(viewModel.uiState.value.showTraffic)
    }

    private class FakeRepository : AppSettingsRepository {
        var snapshot = AppSettingsSnapshot(false, DarkMode.Auto, false, false, true)
        val writes = mutableListOf<String>()

        override fun read() = snapshot
        override fun setAutoRestart(value: Boolean) { writes += "auto:$value" }
        override fun setDarkMode(value: DarkMode) { writes += "dark:$value" }
        override fun setHideAppIcon(value: Boolean) { writes += "icon:$value" }
        override fun setHideFromRecents(value: Boolean) { writes += "recents:$value" }
        override fun setShowTraffic(value: Boolean) { writes += "traffic:$value" }
    }
}
