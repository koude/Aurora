package com.koude.aurora.ui.settings

import com.github.kr328.clash.core.model.ConfigurationOverride
import com.koude.aurora.data.settings.OverrideRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverrideEditorViewModelTest {
    @Test fun editsAreSavedOnlyWhenRequested() = runTest {
        val repository = FakeRepository()
        val editor = OverrideEditorViewModel(repository)
        val configuration = editor.load()

        configuration.httpPort = 7890
        editor.changed()

        assertEquals(1, editor.revision.value)
        assertNull(repository.patched)

        editor.save()
        assertEquals(7890, repository.patched?.httpPort)
    }

    @Test fun resetClearsWithoutSavingTheLoadedConfiguration() = runTest {
        val repository = FakeRepository()
        val editor = OverrideEditorViewModel(repository)
        editor.load().httpPort = 7890

        editor.reset()
        editor.save()

        assertEquals(1, repository.clearCount)
        assertNull(repository.patched)
    }

    private class FakeRepository : OverrideRepository {
        private val source = ConfigurationOverride()
        var patched: ConfigurationOverride? = null
        var clearCount = 0

        override suspend fun query(): ConfigurationOverride = source
        override suspend fun patch(configuration: ConfigurationOverride) {
            patched = configuration.copy()
        }
        override suspend fun clear() {
            clearCount++
        }
    }
}
