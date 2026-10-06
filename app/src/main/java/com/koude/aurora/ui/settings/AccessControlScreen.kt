package com.koude.aurora.ui.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.service.model.AccessControlMode
import com.koude.aurora.ui.components.AuroraDetailTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    apps: List<AppInfo>,
    mode: AccessControlMode,
    selectedPackages: Set<String>,
    hasUnsavedChanges: Boolean,
    applying: Boolean,
    showSystemApps: Boolean,
    sort: AppInfoSort,
    reverse: Boolean,
    onModeChanged: (AccessControlMode) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onSelectInvert: () -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onSortChanged: (AppInfoSort) -> Unit,
    onReverseChanged: (Boolean) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedOnly by remember { mutableStateOf(false) }
    var menuVisible by remember { mutableStateOf(false) }
    var bulkDialog by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val emptyWhitelist = mode == AccessControlMode.AcceptSelected && selectedPackages.isEmpty()
    BackHandler(enabled = hasUnsavedChanges || applying) {
        if (!applying) confirmDiscard = true
    }
    val visibleApps = remember(apps, selectedPackages, selectedOnly, searchText) {
        apps.filter { app ->
            (!selectedOnly || app.packageName in selectedPackages) &&
                (searchText.isBlank() || app.label.contains(searchText, ignoreCase = true) ||
                    app.packageName.contains(searchText, ignoreCase = true))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AuroraDetailTopBar(
                title = stringResource(DesignR.string.aurora_per_app_proxy),
                onBack = {
                    if (applying) Unit
                    else if (hasUnsavedChanges) confirmDiscard = true
                    else onBack()
                },
                actions = {
                    IconButton(onClick = { searchVisible = !searchVisible; searchText = "" }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(DesignR.string.search))
                    }
                    IconButton(onClick = { menuVisible = true }, enabled = !applying) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(DesignR.string.more))
                    }
                    DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.aurora_per_app_batch)) }, onClick = { menuVisible = false; bulkDialog = true })
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.aurora_per_app_list_options)) }, onClick = { menuVisible = false; sortDialog = true })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.import_from_clipboard)) }, onClick = { menuVisible = false; onImport() })
                        DropdownMenuItem(text = { Text(stringResource(DesignR.string.export_to_clipboard)) }, onClick = { menuVisible = false; onExport() })
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (emptyWhitelist) {
                    Text(
                        stringResource(DesignR.string.aurora_per_app_whitelist_empty),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onApply,
                    enabled = hasUnsavedChanges && !emptyWhitelist && !applying,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(if (applying) DesignR.string.aurora_per_app_applying else DesignR.string.aurora_per_app_apply))
                }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(DesignR.string.aurora_per_app_mode),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(if (mode == AccessControlMode.AcceptSelected)
                                DesignR.string.aurora_per_app_whitelist_summary else DesignR.string.aurora_per_app_blacklist_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(
                            AccessControlMode.DenySelected to DesignR.string.aurora_per_app_blacklist,
                            AccessControlMode.AcceptSelected to DesignR.string.aurora_per_app_whitelist,
                        ).forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                enabled = !applying,
                                selected = if (value == AccessControlMode.DenySelected)
                                    mode != AccessControlMode.AcceptSelected else mode == value,
                                onClick = { onModeChanged(value) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(DesignR.string.aurora_per_app_selected_count, selectedPackages.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                FilterChip(
                    selected = selectedOnly,
                    onClick = { selectedOnly = !selectedOnly },
                    label = { Text(stringResource(DesignR.string.aurora_per_app_selected_only)) },
                )
            }
            if (searchVisible) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    singleLine = true,
                    label = { Text(stringResource(DesignR.string.search)) },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 16.dp),
            ) {
                if (visibleApps.isEmpty()) {
                    item {
                        Text(
                            stringResource(DesignR.string.aurora_per_app_no_matches),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(visibleApps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !applying) { onToggleApp(app.packageName) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.icon)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                app.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(checked = app.packageName in selectedPackages, enabled = !applying, onCheckedChange = { onToggleApp(app.packageName) })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
    if (bulkDialog) {
        AlertDialog(
            onDismissRequest = { bulkDialog = false },
            title = { Text(stringResource(DesignR.string.aurora_per_app_batch)) },
            text = {
                Column {
                    listOf(
                        DesignR.string.select_none to onSelectNone,
                        DesignR.string.select_all to onSelectAll,
                        DesignR.string.select_invert to onSelectInvert,
                    ).forEach { (label, action) ->
                        Text(
                            stringResource(label),
                            modifier = Modifier.fillMaxWidth().clickable { bulkDialog = false; action() }.padding(vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { bulkDialog = false }) { Text(stringResource(DesignR.string.close)) } },
        )
    }
    if (sortDialog) {
        AlertDialog(
            onDismissRequest = { sortDialog = false },
            title = { Text(stringResource(DesignR.string.aurora_per_app_list_options)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onShowSystemAppsChanged(!showSystemApps) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(DesignR.string.system_apps), modifier = Modifier.weight(1f))
                        Checkbox(checked = showSystemApps, onCheckedChange = onShowSystemAppsChanged)
                    }
                    HorizontalDivider()
                    Text(
                        stringResource(DesignR.string.aurora_per_app_sort_by),
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    AppInfoSort.values().forEach { value ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSortChanged(value) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(sortLabel(value), modifier = Modifier.weight(1f))
                            RadioButton(selected = sort == value, onClick = { onSortChanged(value) })
                        }
                    }
                    HorizontalDivider()
                    Text(
                        stringResource(DesignR.string.aurora_per_app_sort_order),
                        modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(
                            false to DesignR.string.aurora_per_app_ascending,
                            true to DesignR.string.aurora_per_app_descending,
                        ).forEachIndexed { index, (descending, label) ->
                            SegmentedButton(
                                selected = reverse == descending,
                                onClick = { onReverseChanged(descending) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sortDialog = false }) { Text(stringResource(DesignR.string.close)) } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(DesignR.string.aurora_per_app_discard_title)) },
            text = { Text(stringResource(DesignR.string.aurora_per_app_discard_summary)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) {
                    Text(stringResource(DesignR.string.aurora_per_app_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(DesignR.string.cancel))
                }
            },
        )
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
