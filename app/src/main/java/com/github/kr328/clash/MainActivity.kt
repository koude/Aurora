package com.github.kr328.clash

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.about.AboutScreen
import com.koude.aurora.ui.components.AuroraBottomNavigation
import com.koude.aurora.ui.components.AuroraDestination
import com.koude.aurora.ui.connections.ConnectionsScreen
import com.koude.aurora.ui.connections.ConnectionsViewModel
import com.koude.aurora.ui.home.HomeScreen
import com.koude.aurora.ui.home.HomeViewModel
import com.koude.aurora.ui.navigation.initialMainRoute
import com.koude.aurora.ui.navigation.persistedMainRoutes
import com.koude.aurora.ui.profiles.ProfilesScreen
import com.koude.aurora.ui.profiles.ProfilesViewModel
import com.koude.aurora.ui.proxy.ProxyScreen
import com.koude.aurora.ui.proxy.ProxyViewModel
import com.koude.aurora.ui.settings.SettingsScreen
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.R as DesignR

private data class AuroraErrorDialogState(
    val title: String,
    val details: String,
)

class MainActivity : BaseActivity() {
    private val errorDialogState = mutableStateOf<AuroraErrorDialogState?>(null)
    private val activeRoute = mutableStateOf(ROUTE_HOME)
    private val requestedRoute = mutableStateOf<String?>(null)
    private val profilesViewModel: ProfilesViewModel by viewModels {
        ProfilesViewModel.factory(applicationContext)
    }
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.Factory }
    private val proxyViewModel: ProxyViewModel by viewModels {
        ProxyViewModel.factory(applicationContext)
    }
    private val connectionsViewModel: ConnectionsViewModel by viewModels {
        ConnectionsViewModel.factory()
    }
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override fun onDestroy() {
        homeViewModel.cancelRouteTestRequest()
        super.onDestroy()
    }

    override suspend fun main() {
        setContent { AuroraTheme { AuroraApp() } }

        homeViewModel.refreshConnection(clashRunning)
        proxyViewModel.refresh(clashRunning)
        launch { homeViewModel.errors.collect(::showError) }
        launch { proxyViewModel.errors.collect(::showError) }
        launch { profilesViewModel.errors.collect { showError(it.cause, it.title) } }
        launch {
            profilesViewModel.duplicatedProfiles.collect {
                startActivity(PropertiesActivity::class.intent.setUUID(it))
            }
        }

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))
        while (isActive) {
            select<Unit> {
                events.onReceive { event ->
                    when (event) {
                        Event.ActivityStart, Event.ServiceRecreated, Event.ProfileChanged -> {
                            homeViewModel.refreshConnection(clashRunning)
                            if (activeRoute.value == ROUTE_CONNECTIONS) connectionsViewModel.refresh(clashRunning)
                        }
                        Event.ClashStart -> {
                            homeViewModel.refreshConnection(clashRunning)
                            proxyViewModel.refresh(clashRunning)
                            if (activeRoute.value == ROUTE_CONNECTIONS) connectionsViewModel.refresh(clashRunning)
                        }
                        Event.ClashStop -> {
                            homeViewModel.onServiceStopped()
                            proxyViewModel.onServiceStopped()
                            connectionsViewModel.refresh(false)
                        }
                        Event.ProfileLoaded -> {
                            homeViewModel.refreshConnection(clashRunning)
                            proxyViewModel.onProfileLoaded(clashRunning)
                        }
                        Event.ProfileUpdateCompleted, Event.ProfileUpdateFailed -> profilesViewModel.refresh()
                        else -> Unit
                    }
                }
                if (clashRunning) ticker.onReceive {
                    homeViewModel.refreshTraffic()
                    if (activeRoute.value == ROUTE_CONNECTIONS) connectionsViewModel.refresh(clashRunning)
                }
            }
        }
    }

    @Composable
    private fun AuroraApp() {
        val navController = rememberNavController()
        val startDestination = remember {
            initialMainRoute(uiStore.lastMainRoute)
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destinationRoute = backStackEntry?.destination?.route
        val currentRoute = destinationRoute ?: startDestination
        val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
        val routeTestState by homeViewModel.routeTestState.collectAsStateWithLifecycle()
        val connectionsState by connectionsViewModel.uiState.collectAsStateWithLifecycle()
        val proxyState by proxyViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(requestedRoute.value, backStackEntry) {
            val route = requestedRoute.value
            if (route != null && backStackEntry != null) {
                if (navController.currentDestination?.route != route) {
                    navController.navigateTopLevel(route)
                }
                requestedRoute.value = null
            }
        }

        LaunchedEffect(destinationRoute, homeState.running) {
            val route = destinationRoute ?: return@LaunchedEffect
            activeRoute.value = route
            if (route != ROUTE_HOME && routeTestState.isOpen) {
                homeViewModel.dismissRouteTest()
            }
            proxyViewModel.onRouteChanged(route, ROUTE_PROXY)
            if (route in persistedMainRoutes) {
                uiStore.lastMainRoute = route
            }
            if (route == ROUTE_CONNECTIONS) connectionsViewModel.refresh(clashRunning)
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                AuroraBottomNavigation(
                    selected = currentRoute.toAuroraDestination(),
                    proxyEnabled = homeState.running,
                    onNavigate = { navController.navigateTopLevel(it.route) },
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                enterTransition = { fadeIn(tween(220, delayMillis = 90)) },
                exitTransition = { fadeOut(tween(90)) },
                popEnterTransition = { fadeIn(tween(220, delayMillis = 90)) },
                popExitTransition = { fadeOut(tween(90)) },
            ) {
                composable(ROUTE_HOME) {
                    val profilesState by profilesViewModel.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        state = homeState,
                        profiles = profilesState.profiles,
                        onSelectProfile = profilesViewModel::activate,
                        onToggleConnection = {
                            launch { if (clashRunning) stopClashService() else startClash() }
                        },
                        onModeSelected = homeViewModel::setMode,
                        onTestLatency = {
                            homeViewModel.testAllSiteLatencies()
                        },
                        onTestSiteLatency = homeViewModel::testSiteLatency,
                        routeTestState = routeTestState,
                        onOpenConnections = { navController.navigateTopLevel(ROUTE_CONNECTIONS) },
                        onOpenLogs = {
                            if (LogcatService.running) startActivity(LogcatActivity::class.intent)
                            else startActivity(LogsActivity::class.intent)
                        },
                        onOpenRouteTest = homeViewModel::openRouteTest,
                        onDismissRouteTest = homeViewModel::dismissRouteTest,
                        onRouteTargetChange = homeViewModel::changeRouteTestTarget,
                        onSubmitRouteTest = {
                            homeViewModel.submitRouteTest(
                                serviceRunning = clashRunning,
                                invalidTargetMessage = getString(DesignR.string.aurora_route_invalid_target),
                                serviceRequiredMessage = getString(DesignR.string.aurora_route_service_required),
                                testFailedMessage = getString(DesignR.string.aurora_route_test_failed),
                            )
                        },
                        onOpenDns = { startActivity(NetworkSettingsActivity::class.intent) },
                        onOpenProfiles = { navController.navigate(ROUTE_PROFILES) },
                        onOpenProxy = { navController.navigateTopLevel(ROUTE_PROXY) },
                        onOpenSettings = { navController.navigateTopLevel(ROUTE_SETTINGS) },
                        showBottomNavigation = false,
                    )
                }
                composable(ROUTE_PROXY) {
                    ProxyScreen(
                        state = proxyState,
                        onSelectGroup = proxyViewModel::selectGroup,
                        onGroupExpandedChange = proxyViewModel::setGroupExpanded,
                        onSelectProxy = proxyViewModel::selectProxy,
                        onRefresh = { expandedGroupIndex ->
                            if (expandedGroupIndex == null) proxyViewModel.testActiveEndpoints()
                            else proxyViewModel.testGroup(expandedGroupIndex)
                        },
                        onSortChanged = proxyViewModel::setSort,
                        onHideUnselectableChanged = proxyViewModel::setHideUnselectable,
                    )
                }
                composable(ROUTE_PROFILES) {
                    val state by profilesViewModel.uiState.collectAsStateWithLifecycle()
                    val importProgress by profilesViewModel.importProgress.collectAsStateWithLifecycle()
                    ProfilesScreen(
                        state = state,
                        importProgress = importProgress,
                        onImportFile = { launch { importProfileFromFile() } },
                        onImportUrl = profilesViewModel::importUrl,
                        onScanQrCode = { scanLauncher.launch(null) },
                        onActivateProfile = profilesViewModel::activate,
                        onUpdateProfile = profilesViewModel::update,
                        onEditProfile = { startActivity(PropertiesActivity::class.intent.setUUID(it)) },
                        onDuplicateProfile = profilesViewModel::duplicate,
                        onDeleteProfile = profilesViewModel::delete,
                        onUpdateAll = profilesViewModel::updateAll,
                        proxyEnabled = homeState.running,
                        onBack = { navController.popBackStack() },
                        onOpenHome = { navController.navigateTopLevel(ROUTE_HOME) },
                        onOpenProxy = { navController.navigateTopLevel(ROUTE_PROXY) },
                        onOpenConnections = { navController.navigateTopLevel(ROUTE_CONNECTIONS) },
                        onOpenSettings = { navController.navigateTopLevel(ROUTE_SETTINGS) },
                        showBottomNavigation = false,
                    )
                }
                composable(ROUTE_CONNECTIONS) {
                    ConnectionsScreen(
                        state = connectionsState,
                        onRefresh = { connectionsViewModel.refresh(clashRunning, force = true) },
                        onPoll = { connectionsViewModel.refresh(clashRunning) },
                        onCloseConnection = { id ->
                            connectionsViewModel.close(listOf(id), ::showError)
                        },
                        onCloseVisible = { ids ->
                            connectionsViewModel.close(ids, ::showError)
                        },
                    )
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        proxyEnabled = homeState.running,
                        onOpenHome = { navController.navigateTopLevel(ROUTE_HOME) },
                        onOpenProxy = { navController.navigateTopLevel(ROUTE_PROXY) },
                        onOpenConnections = { navController.navigateTopLevel(ROUTE_CONNECTIONS) },
                        onOpenProfiles = { navController.navigate(ROUTE_PROFILES) },
                        onOpenNetwork = { startActivity(NetworkSettingsActivity::class.intent) },
                        onOpenApp = { startActivity(AppSettingsActivity::class.intent) },
                        onOpenMetaFeature = { startActivity(MetaFeatureSettingsActivity::class.intent) },
                        onOpenOverride = { startActivity(OverrideSettingsActivity::class.intent) },
                        onOpenAbout = { navController.navigate(ROUTE_ABOUT) },
                        showBottomNavigation = false,
                    )
                }
                composable(ROUTE_ABOUT) {
                    AboutScreen(
                        versionName = BuildConfig.VERSION_NAME,
                        onOpenLink = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        errorDialogState.value?.let { error ->
            AuroraErrorDialog(
                error = error,
                onDismiss = { errorDialogState.value = null },
            )
        }
    }

    private fun NavHostController.navigateTopLevel(route: String) {
        navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.startDestinationId) { saveState = true }
        }
    }

    private suspend fun startClash() {
        when (homeViewModel.canStartConnection()) {
            true -> Unit
            false -> {
                Toast.makeText(this@MainActivity, DesignR.string.no_profile_selected, Toast.LENGTH_LONG).show()
                requestedRoute.value = ROUTE_PROFILES
                return
            }
            null -> return
        }
        val vpnRequest = startClashService()
        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(ActivityResultContracts.StartActivityForResult(), vpnRequest)
                if (result.resultCode == RESULT_OK) startClashService()
            }
        } catch (_: Exception) {
            Toast.makeText(
                this@MainActivity,
                DesignR.string.unable_to_start_vpn,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private suspend fun importProfileFromFile() {
        val uri: Uri = startActivityForResult(ActivityResultContracts.GetContent(), "*/*") ?: return
        val name = uri.fileName?.substringBeforeLast('.')?.takeIf(String::isNotBlank)
            ?: getString(DesignR.string.new_profile)
        profilesViewModel.importFile(name, uri.toString())
    }

    private fun scanResultHandler(result: QRResult) {
        when (result) {
            is QRResult.QRSuccess -> {
                val url = result.content.rawValue
                    ?: result.content.rawBytes?.let(::String).orEmpty()
                if (url.isNotBlank()) profilesViewModel.importUrl(
                    getString(DesignR.string.new_profile), url,
                )
            }
            QRResult.QRUserCanceled -> Unit
            QRResult.QRMissingPermission -> Toast.makeText(
                this@MainActivity,
                DesignR.string.import_from_qr_no_permission,
                Toast.LENGTH_LONG,
            ).show()
            is QRResult.QRError -> Toast.makeText(
                this@MainActivity,
                DesignR.string.import_from_qr_exception,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun showError(error: Throwable) {
        showError(error, "操作失败")
    }

    private fun showError(error: Throwable, title: String) {
        val messages = mutableListOf<String>()
        val visited = mutableSetOf<Throwable>()
        var current: Throwable? = error
        while (current != null && visited.add(current)) {
            current.message
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(messages::add)
            current = current.cause
        }
        errorDialogState.value = AuroraErrorDialogState(
            title = title,
            details = messages.distinct().joinToString("\n\n").ifBlank { "未提供错误详情" },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedRoute.value = routeFromIntent(intent)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(RequestPermission()) { }
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setupShortcuts()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRoute.value = routeFromIntent(intent)
    }

    private fun routeFromIntent(intent: Intent): String? =
        intent.getStringExtra(EXTRA_TOP_LEVEL_ROUTE)
            ?.takeIf { it in TOP_LEVEL_ROUTES }
            ?: if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES ||
                intent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES"
            ) ROUTE_SETTINGS else null

    private fun setupShortcuts() {
        if (uiStore.hideAppIcon) return
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
            Intent.FLAG_ACTIVITY_NO_ANIMATION

        fun shortcut(id: String, short: Int, long: Int, icon: Int, action: String, rank: Int) =
            ShortcutInfoCompat.Builder(this, id)
                .setShortLabel(getString(short))
                .setLongLabel(getString(long))
                .setIcon(IconCompat.createWithResource(this, icon))
                .setIntent(
                    Intent(action)
                        .setClassName(this, ExternalControlActivity::class.java.name)
                        .addFlags(flags),
                )
                .setRank(rank)
                .build()

        ShortcutManagerCompat.setDynamicShortcuts(
            this,
            listOf(
                shortcut("toggle_clash", DesignR.string.shortcut_toggle_short, DesignR.string.shortcut_toggle_long, R.drawable.ic_toggle_all, Intents.ACTION_TOGGLE_CLASH, 0),
                shortcut("start_clash", DesignR.string.shortcut_start_short, DesignR.string.shortcut_start_long, R.drawable.ic_toggle_on, Intents.ACTION_START_CLASH, 1),
                shortcut("stop_clash", DesignR.string.shortcut_stop_short, DesignR.string.shortcut_stop_long, R.drawable.ic_toggle_off, Intents.ACTION_STOP_CLASH, 2),
            ),
        )
    }

    companion object {
        const val EXTRA_TOP_LEVEL_ROUTE = "com.koude.aurora.extra.TOP_LEVEL_ROUTE"
        const val ROUTE_HOME = "home"
        const val ROUTE_PROXY = "proxy"
        const val ROUTE_CONNECTIONS = "connections"
        const val ROUTE_PROFILES = "profiles"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_ABOUT = "about"
        private val TOP_LEVEL_ROUTES = setOf(ROUTE_HOME, ROUTE_PROXY, ROUTE_CONNECTIONS, ROUTE_PROFILES, ROUTE_SETTINGS)
    }
}

private val AuroraDestination.route: String
    get() = when (this) {
        AuroraDestination.Home -> MainActivity.ROUTE_HOME
        AuroraDestination.Proxy -> MainActivity.ROUTE_PROXY
        AuroraDestination.Connections -> MainActivity.ROUTE_CONNECTIONS
        AuroraDestination.Settings -> MainActivity.ROUTE_SETTINGS
    }

private fun String.toAuroraDestination(): AuroraDestination = when (this) {
    MainActivity.ROUTE_PROXY -> AuroraDestination.Proxy
    MainActivity.ROUTE_CONNECTIONS -> AuroraDestination.Connections
    MainActivity.ROUTE_PROFILES -> AuroraDestination.Settings
    MainActivity.ROUTE_SETTINGS -> AuroraDestination.Settings
    MainActivity.ROUTE_ABOUT -> AuroraDestination.Settings
    else -> AuroraDestination.Home
}

@Composable
private fun AuroraErrorDialog(
    error: AuroraErrorDialogState,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(error.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "操作未能完成。错误详情如下：",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        text = error.details,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(error.details))
                    onDismiss()
                },
            ) {
                Text("复制详情")
            }
        },
    )
}
