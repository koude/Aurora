package com.koude.aurora.ui.profiles

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.ProfileProvider
import com.koude.aurora.ui.components.AuroraDetailTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewProfileScreen(
    providers: List<ProfileProvider>,
    onBack: () -> Unit,
    onSelect: (ProfileProvider) -> Unit,
    onProviderDetails: (ProfileProvider.External) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(topBar = { AuroraDetailTopBar(title = stringResource(DesignR.string.create_profile), onBack = onBack) }) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(providers) { provider ->
                val icon = when (provider) {
                    is ProfileProvider.File -> DesignR.drawable.ic_baseline_attach_file
                    is ProfileProvider.Url -> DesignR.drawable.ic_baseline_cloud_download
                    is ProfileProvider.QR -> DesignR.drawable.baseline_qr_code_scanner
                    is ProfileProvider.External -> null
                }
                Card(
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = { onSelect(provider) },
                        onLongClick = { if (provider is ProfileProvider.External) onProviderDetails(provider) },
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        else provider.icon?.let { drawable ->
                            Image(BitmapPainter(drawable.toBitmap(width = 48, height = 48).asImageBitmap()), contentDescription = null)
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(provider.name, style = MaterialTheme.typography.titleMedium)
                            Text(provider.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
