package com.koude.aurora.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.kr328.clash.core.model.ConnectionInfo
import com.github.kr328.clash.core.model.Proxy
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.connections.ConnectionsScreen
import com.koude.aurora.ui.connections.ConnectionsUiState
import com.koude.aurora.ui.home.HomeScreen
import com.koude.aurora.ui.home.HomeUiState
import com.koude.aurora.ui.home.RouteTestUiState
import com.koude.aurora.ui.profiles.ProfilesScreen
import com.koude.aurora.ui.profiles.ProfilesUiState
import com.koude.aurora.ui.proxy.ProxyScreen
import com.koude.aurora.ui.proxy.ProxyGroupUiState
import com.koude.aurora.ui.proxy.ProxyUiState
import com.koude.aurora.ui.settings.SettingsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopLevelScreenSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeShowsLatencyAndPeerNavigation() {
        var openedConnections = false
        compose.setContent {
            AuroraTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onToggleConnection = {},
                    onModeSelected = {},
                    onTestLatency = {},
                    onTestSiteLatency = {},
                    routeTestState = RouteTestUiState(),
                    onOpenConnections = { openedConnections = true },
                    onOpenLogs = {},
                    onOpenRouteTest = {},
                    onDismissRouteTest = {},
                    onRouteTargetChange = {},
                    onSubmitRouteTest = {},
                    onOpenDns = {},
                    onOpenProfiles = {},
                    onOpenProxy = {},
                    onOpenSettings = {},
                )
            }
        }
        compose.onNodeWithText("网站延迟").assertIsDisplayed()
        compose.onNodeWithText("Apple").assertIsDisplayed()
        compose.onNodeWithTag("bottom_navigation_connections").performClick()
        compose.runOnIdle { assertTrue(openedConnections) }
    }

    @Test fun proxyShowsDisconnectedState() {
        compose.setContent {
            AuroraTheme {
                ProxyScreen(
                    state = ProxyUiState(),
                    onSelectGroup = {},
                    onGroupExpandedChange = { _, _ -> },
                    onSelectProxy = { _, _ -> },
                    onRefresh = {},
                    onSortChanged = {},
                    onHideUnselectableChanged = {},
                )
            }
        }
        compose.onNodeWithText("代理服务未连接").assertIsDisplayed()
    }

    @Test fun proxyRefreshTargetsVisibleScope() {
        val expanded = mutableStateOf(false)
        val refreshTargets = mutableListOf<Int?>()
        compose.setContent {
            AuroraTheme {
                ProxyScreen(
                    state = ProxyUiState(
                        serviceRunning = true,
                        groups = listOf(
                            ProxyGroupUiState(
                                name = "测试策略组",
                                selectedProxy = "测试节点",
                                proxies = listOf(Proxy("测试节点", "测试节点", "", "SS", 65535, false)),
                            ),
                        ),
                        expandedGroups = mapOf("测试策略组" to expanded.value),
                    ),
                    onSelectGroup = {},
                    onGroupExpandedChange = { _, value -> expanded.value = value },
                    onSelectProxy = { _, _ -> },
                    onRefresh = { refreshTargets.add(it) },
                    onSortChanged = {},
                    onHideUnselectableChanged = {},
                )
            }
        }
        compose.onNodeWithContentDescription("测速").performClick()
        compose.onNodeWithText("测试策略组").performClick()
        compose.onNodeWithContentDescription("测速").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf(null, 0), refreshTargets) }
    }

    @Test fun connectionsSearchOpensAndCloseIsDisabledWhenEmpty() {
        compose.setContent {
            AuroraTheme {
                ConnectionsScreen(
                    state = ConnectionsUiState(),
                    onRefresh = {},
                    onCloseConnection = {},
                    onCloseVisible = {},
                )
            }
        }
        compose.onNodeWithContentDescription("关闭所示连接").assertIsNotEnabled()
        compose.onNodeWithContentDescription("搜索连接").performClick()
        compose.onNodeWithContentDescription("关闭搜索").assertIsDisplayed()
    }

    @Test fun connectionsCloseOnlyFilteredRows() {
        var closedIds = emptyList<String>()
        compose.setContent {
            AuroraTheme {
                ConnectionsScreen(
                    state = ConnectionsUiState(
                        serviceRunning = true,
                        connections = listOf(
                            ConnectionInfo(id = "github", host = "github.com"),
                            ConnectionInfo(id = "video", host = "youtube.com"),
                        ),
                    ),
                    onRefresh = {},
                    onCloseConnection = {},
                    onCloseVisible = { closedIds = it },
                )
            }
        }
        compose.onNodeWithContentDescription("搜索连接").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("github")
        compose.onNodeWithText("github.com").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭所示连接").performClick()
        compose.onNodeWithText("将中断列表中显示的 1 条连接。").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf("github"), closedIds) }
    }

    @Test fun settingsOpensProfiles() {
        var openedProfiles = false
        compose.setContent {
            AuroraTheme {
                SettingsScreen(
                    proxyEnabled = false,
                    onOpenHome = {},
                    onOpenProxy = {},
                    onOpenProfiles = { openedProfiles = true },
                    onOpenNetwork = {},
                    onOpenApp = {},
                    onOpenMetaFeature = {},
                    onOpenOverride = {},
                )
            }
        }
        compose.onNodeWithText("配置").performClick()
        compose.runOnIdle { assertTrue(openedProfiles) }
    }

    @Test fun emptyProfilesShowOneAddEntryAndSources() {
        compose.setContent {
            AuroraTheme {
                ProfilesScreen(
                    state = ProfilesUiState(loading = false),
                    onImportFile = {},
                    onImportUrl = { _, _ -> },
                    onScanQrCode = {},
                    onActivateProfile = {},
                    onUpdateProfile = {},
                    onEditProfile = {},
                    onDuplicateProfile = {},
                    onDeleteProfile = {},
                    onUpdateAll = {},
                    proxyEnabled = false,
                    onOpenHome = {},
                    onOpenProxy = {},
                    onOpenSettings = {},
                )
            }
        }
        compose.onNodeWithText("还没有配置").assertIsDisplayed()
        compose.onNodeWithText("添加配置", useUnmergedTree = true).performClick()
        compose.onNodeWithText("本地文件").assertIsDisplayed()
        compose.onNodeWithText("远程订阅").assertIsDisplayed()
        compose.onNodeWithText("扫描二维码").assertIsDisplayed()
    }
}
