package com.github.kr328.clash

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.util.withClash
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.providers.ProviderRowState
import com.koude.aurora.ui.providers.ProvidersScreen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.TimeUnit

class ProvidersActivity : BaseActivity() {
    private val providerRows = mutableStateOf<List<ProviderRowState>>(emptyList())
    private val currentTime = mutableStateOf(System.currentTimeMillis())

    override suspend fun main() {
        providerRows.value = withClash { queryProviders().sorted() }.map { ProviderRowState(it, it.updatedAt) }
        setContent {
            AuroraTheme {
                ProvidersScreen(
                    providers = providerRows.value,
                    now = currentTime.value,
                    onUpdate = ::updateProvider,
                    onBack = ::finish,
                )
            }
        }

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))
        while (isActive) {
            select<Unit> {
                events.onReceive {
                    if (it == Event.ProfileLoaded) {
                        providerRows.value = withClash { queryProviders().sorted() }.map { ProviderRowState(it, it.updatedAt) }
                    }
                }
                if (activityStarted) ticker.onReceive { currentTime.value = System.currentTimeMillis() }
            }
        }
    }

    private fun updateProvider(index: Int) {
        val row = providerRows.value.getOrNull(index) ?: return
        if (row.updating || row.provider.vehicleType == Provider.VehicleType.Inline) return
        val key = row.provider.name to row.provider.type
        providerRows.value = providerRows.value.toMutableList().also { it[index] = row.copy(updating = true) }

        launch {
            try {
                withClash { updateProvider(row.provider.type, row.provider.name) }
                updateRow(key) { it.copy(updating = false, updatedAt = System.currentTimeMillis()) }
            } catch (e: Exception) {
                updateRow(key) { it.copy(updating = false) }
                Toast.makeText(
                    this@ProvidersActivity,
                    getString(DesignR.string.format_update_provider_failure, row.provider.name, e.message ?: ""),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun updateRow(key: Pair<String, Provider.Type>, transform: (ProviderRowState) -> ProviderRowState) {
        providerRows.value = providerRows.value.map { row ->
            if (row.provider.name == key.first && row.provider.type == key.second) transform(row) else row
        }
    }
}
