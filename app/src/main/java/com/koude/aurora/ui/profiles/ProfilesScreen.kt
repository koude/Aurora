package com.koude.aurora.ui.profiles

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.model.ProfileKind
import com.koude.aurora.model.ProfileSummary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    state: ProfilesUiState,
    onImportFile: () -> Unit,
    onImportUrl: (name: String, url: String) -> Unit,
    onScanQrCode: () -> Unit,
    onOpenMoreSources: () -> Unit,
    onOpenProfile: (UUID) -> Unit,
    onActivateProfile: (UUID) -> Unit,
    onUpdateAll: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenProxy: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var addSheetVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            ProfilesNavigationBar(
                onOpenHome = onOpenHome,
                onOpenProxy = onOpenProxy,
                onOpenSettings = onOpenSettings,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.height(54.dp),
                onClick = { addSheetVisible = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加配置") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            state.profiles.isEmpty() -> EmptyContent(
                modifier = Modifier.padding(padding),
                onAddProfile = { addSheetVisible = true },
            )
            else -> ProfileList(
                profiles = state.profiles,
                errorMessage = state.errorMessage,
                onOpenProfile = onOpenProfile,
                onActivateProfile = onActivateProfile,
                onUpdateAll = onUpdateAll,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = padding.calculateTopPadding() + 18.dp,
                    end = 20.dp,
                    bottom = padding.calculateBottomPadding() + 104.dp,
                ),
            )
        }
    }

    if (addSheetVisible) {
        AddProfileSheet(
            onDismiss = { addSheetVisible = false },
            onImportFile = {
                addSheetVisible = false
                onImportFile()
            },
            onImportUrl = { name, url ->
                addSheetVisible = false
                onImportUrl(name, url)
            },
            onScanQrCode = {
                addSheetVisible = false
                onScanQrCode()
            },
            onOpenMoreSources = {
                addSheetVisible = false
                onOpenMoreSources()
            },
        )
    }
}

@Composable
private fun ProfileList(
    profiles: List<ProfileSummary>,
    errorMessage: String?,
    onOpenProfile: (UUID) -> Unit,
    onActivateProfile: (UUID) -> Unit,
    onUpdateAll: () -> Unit,
    contentPadding: PaddingValues,
) {
    val activeProfile = profiles.firstOrNull(ProfileSummary::active)
    val otherProfiles = profiles.filterNot(ProfileSummary::active)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProfilesHeader(onUpdateAll = onUpdateAll) }

        item { Spacer(Modifier.height(4.dp)) }

        if (activeProfile != null) {
            item {
                ActiveProfileCard(
                    profile = activeProfile,
                    onOpen = { onOpenProfile(activeProfile.id) },
                )
            }
        } else {
            item { NoActiveProfileCard() }
        }

        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (otherProfiles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "其他配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${otherProfiles.size} 个可切换",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(otherProfiles, key = ProfileSummary::id) { profile ->
                ProfileCard(
                    profile = profile,
                    onOpen = { onOpenProfile(profile.id) },
                    onActivate = { onActivateProfile(profile.id) },
                )
            }
        }
    }
}

@Composable
private fun ProfilesHeader(onUpdateAll: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "配置",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "选择 Aurora 当前使用的规则与节点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多配置操作")
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("更新所有订阅") },
                    onClick = {
                        menuExpanded = false
                        onUpdateAll()
                    },
                )
            }
        }
    }
}

private enum class AddProfileSheetMode {
    Sources,
    Url,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProfileSheet(
    onDismiss: () -> Unit,
    onImportFile: () -> Unit,
    onImportUrl: (name: String, url: String) -> Unit,
    onScanQrCode: () -> Unit,
    onOpenMoreSources: () -> Unit,
) {
    var mode by remember { mutableStateOf(AddProfileSheetMode.Sources) }
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val urlValid = url.startsWith("https://") || url.startsWith("http://")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(
                text = if (mode == AddProfileSheetMode.Sources) "添加配置" else "订阅 URL",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (mode == AddProfileSheetMode.Sources) {
                    "选择来源，下一步只显示必要信息。"
                } else {
                    "为订阅填写名称和完整地址。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            if (mode == AddProfileSheetMode.Sources) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileSourceButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.List,
                        label = "本地文件",
                        onClick = onImportFile,
                    )
                    ProfileSourceButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Menu,
                        label = "订阅 URL",
                        onClick = { mode = AddProfileSheetMode.Url },
                    )
                    ProfileSourceButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Add,
                        label = "二维码",
                        onClick = onScanQrCode,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onOpenMoreSources,
                ) {
                    Text("更多来源")
                }
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("订阅地址") },
                    placeholder = { Text("https://example.com/profile") },
                    singleLine = true,
                    isError = url.isNotBlank() && !urlValid,
                    supportingText = if (url.isNotBlank() && !urlValid) {
                        { Text("请输入以 http:// 或 https:// 开头的地址") }
                    } else {
                        null
                    },
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { mode = AddProfileSheetMode.Sources }) {
                        Text("返回")
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        enabled = name.isNotBlank() && urlValid,
                        onClick = { onImportUrl(name.trim(), url.trim()) },
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSourceButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(94.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(9.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ActiveProfileCard(
    profile: ProfileSummary,
    onOpen: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val primary = MaterialTheme.colorScheme.primary
    val gradient = Brush.linearGradient(
        colors = listOf(
            primary.copy(red = (primary.red + 0.10f).coerceAtMost(1f)),
            primary.copy(
                red = primary.red * 0.72f,
                green = primary.green * 0.72f,
                blue = primary.blue * 0.88f,
            ),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 142.dp)
            .clip(shape)
            .background(gradient)
            .clickable(onClick = onOpen),
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(154.dp),
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.minDimension * 0.56f,
                center = Offset(size.width * 0.76f, size.height * 0.05f),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(18.dp),
                            shape = CircleShape,
                            color = Color(0xFFBFFFC9).copy(alpha = 0.15f),
                        ) {}
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Color(0xFFBFFFC9),
                        ) {}
                    }
                    Text(
                        text = "当前启用",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.90f),
                    )
                }
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.11f),
                    contentColor = Color.White,
                ) {
                    IconButton(onClick = onOpen) {
                        Icon(Icons.Default.MoreVert, contentDescription = "配置操作")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = profile.detailText(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun NoActiveProfileCard() {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text("尚未启用配置", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "从下方选择一个已经导入的配置。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileSummary,
    onOpen: () -> Unit,
    onActivate: () -> Unit,
) {
    Card(
        onClick = if (profile.imported) onActivate else onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (profile.kind == ProfileKind.File) Icons.AutoMirrored.Filled.List else Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = profile.detailText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = if (profile.imported) onActivate else onOpen) {
                SwitchProfileIcon(
                    contentDescription = if (profile.imported) "切换到${profile.name}" else "继续设置${profile.name}",
                )
            }
        }
    }
}

@Composable
private fun SwitchProfileIcon(contentDescription: String) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val stroke = 1.8.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.34f),
            end = Offset(size.width * 0.82f, size.height * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.82f, size.height * 0.34f),
            end = Offset(size.width * 0.66f, size.height * 0.18f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.82f, size.height * 0.34f),
            end = Offset(size.width * 0.66f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.82f, size.height * 0.68f),
            end = Offset(size.width * 0.18f, size.height * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.68f),
            end = Offset(size.width * 0.34f, size.height * 0.52f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.68f),
            end = Offset(size.width * 0.34f, size.height * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ProfilesNavigationBar(
    onOpenHome: () -> Unit,
    onOpenProxy: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        NavigationItem("首页", Icons.Default.Home, false, onOpenHome)
        NavigationItem("代理", Icons.AutoMirrored.Filled.List, false, onOpenProxy)
        NavigationItem("配置", Icons.Default.Menu, true, {})
        NavigationItem("设置", Icons.Default.Settings, false, onOpenSettings)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(
    modifier: Modifier = Modifier,
    onAddProfile: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("还没有配置", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "添加本地文件或订阅地址后，配置会显示在这里。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        ExtendedFloatingActionButton(
            onClick = onAddProfile,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("添加配置") },
        )
    }
}

private fun ProfileSummary.detailText(): String {
    val source = when (kind) {
        ProfileKind.File -> "本地文件"
        ProfileKind.Url -> "URL 订阅"
        ProfileKind.External -> "外部来源"
    }
    if (!imported) return "$source · 等待保存"

    val updated = when {
        updatedAt <= 0L -> "已就绪"
        System.currentTimeMillis() - updatedAt < DateUtils.MINUTE_IN_MILLIS -> "刚刚更新"
        else -> "${DateUtils.getRelativeTimeSpanString(
            updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        )}更新"
    }
    return "$source · $updated"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilesScreenPreview() {
    AuroraTheme {
        ProfilesScreen(
            state = ProfilesUiState(
                loading = false,
                profiles = listOf(
                    ProfileSummary(UUID(0, 1), "日常使用", ProfileKind.Url, true, true, 0),
                    ProfileSummary(UUID(0, 2), "备用线路", ProfileKind.File, false, true, 0),
                    ProfileSummary(UUID(0, 3), "出差模式", ProfileKind.Url, false, true, 0),
                ),
            ),
            onImportFile = {},
            onImportUrl = { _, _ -> },
            onScanQrCode = {},
            onOpenMoreSources = {},
            onOpenProfile = {},
            onActivateProfile = {},
            onUpdateAll = {},
            onOpenHome = {},
            onOpenProxy = {},
            onOpenSettings = {},
        )
    }
}
