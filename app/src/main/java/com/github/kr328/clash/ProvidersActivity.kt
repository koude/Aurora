package com.github.kr328.clash

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.providers.ProvidersScreen
import com.koude.aurora.ui.providers.ProvidersViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.TimeUnit

class ProvidersActivity : BaseActivity() {
    private val viewModel: ProvidersViewModel by viewModels { ProvidersViewModel.Factory }
    private val currentTime = mutableStateOf(System.currentTimeMillis())

    override suspend fun main() {
        launch {
            viewModel.errors.collect { error ->
                Toast.makeText(
                    this@ProvidersActivity,
                    getString(DesignR.string.format_update_provider_failure, error.name, error.message),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        viewModel.refresh()
        setContent {
            AuroraTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                val providerRows by viewModel.rows.collectAsStateWithLifecycle()
                ProvidersScreen(
                    providers = providerRows,
                    now = currentTime.value,
                    onUpdate = viewModel::update,
                    onBack = ::finish,
                )
            }
        }

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))
        while (isActive) {
            select<Unit> {
                events.onReceive {
                    if (it == Event.ProfileLoaded) {
                        viewModel.refresh()
                    }
                }
                if (activityStarted) ticker.onReceive { currentTime.value = System.currentTimeMillis() }
            }
        }
    }

}
