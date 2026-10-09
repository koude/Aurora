package com.github.kr328.clash

import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.util.ApplicationObserver
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.AppSettingsScreen
import com.koude.aurora.ui.settings.AppSettingsViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AppSettingsActivity : BaseActivity() {
    private val viewModel: AppSettingsViewModel by viewModels {
        AppSettingsViewModel.factory(applicationContext)
    }

    override suspend fun main() {
        viewModel.refresh(clashRunning)

        setContent {
            AuroraTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                val screenState by viewModel.uiState.collectAsStateWithLifecycle()
                AppSettingsScreen(
                    state = screenState,
                    onAutoRestartChanged = viewModel::setAutoRestart,
                    onDarkModeChanged = {
                        viewModel.setDarkMode(it)
                        ApplicationObserver.createdActivities.forEach { activity -> activity.recreate() }
                    },
                    onDynamicColorChanged = {
                        viewModel.setDynamicColor(it)
                        ApplicationObserver.createdActivities.forEach { activity -> activity.recreate() }
                    },
                    onHideAppIconChanged = viewModel::setHideAppIcon,
                    onHideFromRecentsChanged = {
                        viewModel.setHideFromRecents(it)
                        ApplicationObserver.createdActivities.forEach { activity -> activity.recreate() }
                    },
                    onShowTrafficChanged = viewModel::setShowTraffic,
                    onBack = ::finish,
                )
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart, Event.ClashStop, Event.ServiceRecreated -> {
                            viewModel.refresh(clashRunning)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

}
