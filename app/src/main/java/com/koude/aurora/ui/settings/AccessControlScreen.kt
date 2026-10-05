package com.koude.aurora.ui.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.koude.aurora.ui.components.AuroraDetailTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    apps: List<AppInfo>,
    selectedPackages: Set<String>,
    showSystemApps: Boolean,
    sort: AppInfoSort,
    reverse: Boolean,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onSelectInvert: () -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onSortChanged: (AppInfoSort) -> Unit,
    onReverseChanged: (Boolean) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var menuVisible by remember { mutableStateOf(false) }
    val visibleApps = remember(apps, searchText) {
        if (searchText.isBlank()) apps else apps.filter {
            it.label.contains(searchText, ignoreCase = true) || it.packageName.contains(searchText, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AuroraDetailTopBar(
                title = stringResource(DesignR.string.access_control_packages),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { searchVisible = !searchVisible; searchText = "" }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(DesignR.string.search))
                    }
                    IconButton(onClick = { menuVisible = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(DesignR.string.more))
                    }
                    DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.select_all)) }, onClick = { menuVisible = false; onSelectAll() })
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.select_none)) }, onClick = { menuVisible = false; onSelectNone() })
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.select_invert)) }, onClick = { menuVisible = false; onSelectInvert() })
                        DropdownMenuItem(
                            text = { Text(stringResource(DesignR.string.system_apps) + if (showSystemApps) " ✓" else "") },
                            onClick = { menuVisible = false; onShowSystemAppsChanged(!showSystemApps) },
                        )
                        AppInfoSort.values().forEach { value ->
                            DropdownMenuItem(
                                text = { Text((if (sort == value) "✓  " else "") + sortLabel(value)) },
                                onClick = { menuVisible = false; onSortChanged(value) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(DesignR.string.reverse) + if (reverse) " ✓" else "") },
                            onClick = { menuVisible = false; onReverseChanged(!reverse) },
                        )
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.import_from_clipboard)) }, onClick = { menuVisible = false; onImport() })
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.export_to_clipboard)) }, onClick = { menuVisible = false; onExport() })
                    }
                },
            )
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (searchVisible) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text(stringResource(DesignR.string.search)) },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 16.dp),
            ) {
                items(visibleApps, key = { it.packageName }) { app ->
                    ListItem(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleApp(app.packageName) },
                        leadingContent = { AppIcon(app.icon) },
                        headlineContent = { Text(app.label, maxLines = 1) },
                        supportingContent = { Text(app.packageName, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                        trailingContent = { Checkbox(checked = app.packageName in selectedPackages, onCheckedChange = { onToggleApp(app.packageName) }) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(drawable: Drawable) {
    val bitmap = remember(drawable) {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
            val canvas = Canvas(output)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }.asImageBitmap()
    }
    androidx.compose.foundation.Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
}

@Composable
private fun sortLabel(sort: AppInfoSort): String = stringResource(when (sort) {
    AppInfoSort.Label -> DesignR.string.name
    AppInfoSort.PackageName -> DesignR.string.package_name
    AppInfoSort.InstallTime -> DesignR.string.install_time
    AppInfoSort.UpdateTime -> DesignR.string.update_time
})
