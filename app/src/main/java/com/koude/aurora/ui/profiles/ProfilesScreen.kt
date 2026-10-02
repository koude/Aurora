package com.koude.aurora.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.model.ProfileKind
import com.koude.aurora.model.ProfileSummary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    state: ProfilesUiState,
    onAddProfile: () -> Unit,
    onOpenProfile: (UUID) -> Unit,
    onActivateProfile: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(title = { Text("配置") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProfile,
                icon = { androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加配置") },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            state.profiles.isEmpty() -> EmptyContent(Modifier.padding(padding))
            else -> ProfileList(
                profiles = state.profiles,
                onOpenProfile = onOpenProfile,
                onActivateProfile = onActivateProfile,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
            )
        }
    }
}

@Composable
private fun ProfileList(
    profiles: List<ProfileSummary>,
    onOpenProfile: (UUID) -> Unit,
    onActivateProfile: (UUID) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(profiles, key = ProfileSummary::id) { profile ->
            ProfileCard(
                profile = profile,
                onClick = { onOpenProfile(profile.id) },
                onActivate = { onActivateProfile(profile.id) },
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileSummary,
    onClick: () -> Unit,
    onActivate: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (profile.active) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = profile.subtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profile.active) {
                Text(
                    text = "使用中",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (profile.imported) {
                TextButton(onClick = onActivate) {
                    Text("启用")
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
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
    }
}

private fun ProfileSummary.subtitle(): String {
    val source = when (kind) {
        ProfileKind.File -> "本地文件"
        ProfileKind.Url -> "URL 订阅"
        ProfileKind.External -> "外部来源"
    }
    return if (imported) source else "$source · 等待保存"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilesScreenPreview() {
    AuroraTheme {
        ProfilesScreen(
            state = ProfilesUiState(
                loading = false,
                profiles = listOf(
                    ProfileSummary(
                        id = UUID(0, 1),
                        name = "日常使用",
                        kind = ProfileKind.Url,
                        active = true,
                        imported = true,
                        updatedAt = 0,
                    ),
                    ProfileSummary(
                        id = UUID(0, 2),
                        name = "备用配置",
                        kind = ProfileKind.File,
                        active = false,
                        imported = true,
                        updatedAt = 0,
                    ),
                ),
            ),
            onAddProfile = {},
            onOpenProfile = {},
            onActivateProfile = {},
        )
    }
}
