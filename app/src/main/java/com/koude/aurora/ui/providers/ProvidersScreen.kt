package com.koude.aurora.ui.providers

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.util.type

data class ProviderRowState(
    val provider: Provider,
    val updatedAt: Long,
    val updating: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    providers: List<ProviderRowState>,
    now: Long,
    onUpdate: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(DesignR.string.providers), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(providers.indices.toList(), key = { providers[it].provider.name + providers[it].provider.type.name }) { index ->
                val row = providers[index]
                ProviderCard(row, now, onUpdate = { onUpdate(index) })
            }
        }
    }
}

@Composable
private fun ProviderCard(row: ProviderRowState, now: Long, onUpdate: () -> Unit) {
    val context = LocalContext.current
    val inline = row.provider.vehicleType == Provider.VehicleType.Inline
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        ListItem(
            headlineContent = { Text(row.provider.name, maxLines = 1) },
            supportingContent = {
                androidx.compose.foundation.layout.Column {
                    Text(row.provider.type(context), style = MaterialTheme.typography.bodySmall)
                    if (!inline) {
                        Text(
                            DateUtils.getRelativeTimeSpanString(row.updatedAt, now, DateUtils.MINUTE_IN_MILLIS).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            trailingContent = {
                if (!inline) {
                    if (row.updating) CircularProgressIndicator(Modifier.padding(12.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = onUpdate) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(DesignR.string.update))
                    }
                }
            },
        )
    }
}
