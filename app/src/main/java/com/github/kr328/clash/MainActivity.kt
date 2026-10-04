package com.github.kr328.clash

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficDownload
import com.github.kr328.clash.core.util.trafficUpload
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraBottomNavigation
import com.koude.aurora.ui.components.AuroraDestination
import com.koude.aurora.ui.connections.ConnectionsScreen
import com.koude.aurora.ui.connections.ConnectionsUiState
import com.koude.aurora.ui.home.HomeScreen
import com.koude.aurora.ui.home.HomeUiState
import com.koude.aurora.ui.profiles.ProfilesScreen
import com.koude.aurora.ui.profiles.ProfilesViewModel
import com.koude.aurora.ui.proxy.ProxyGroupUiState
import com.koude.aurora.ui.proxy.ProxyRouteUiState
import com.koude.aurora.ui.proxy.ProxyScreen
import com.koude.aurora.ui.proxy.ProxyUiState
import com.koude.aurora.ui.settings.SettingsScreen
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.R as DesignR

class MainActivity : BaseActivity<MainDesign>() {
    private var latencyTestJob: Job? = null
    private var proxyRefreshJob: Job? = null
    private val testedProxyGroups = mutableSetOf<String>()
    private val testedActiveProxies = mutableSetOf<String>()
    private val homeUiState = mutableStateOf(HomeUiState())
    private val proxyUiState = mutableStateOf(ProxyUiState())
    private val connectionsUiState = mutableStateOf(ConnectionsUiState())
    private val activeRoute = mutableStateOf(ROUTE_HOME)
    private val requestedRoute = mutableStateOf<String?>(null)
    private val profilesViewModel: ProfilesViewModel by viewModels { ProfilesViewModel.Factory }
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override suspend fun main() {
        val design = MainDesign(this)

        setContentDesign(design)
        setContent { AuroraTheme { AuroraApp(design) } }

        design.fetch()
        refreshProxy()

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))
        while (isActive) {
            select<Unit> {
                events.onReceive { event ->
                    when (event) {
                        Event.ActivityStart, Event.ServiceRecreated, Event.ProfileChanged -> {
                            design.fetch()
                            if (activeRoute.value == ROUTE_CONNECTIONS) refreshConnections()
                        }
                        Event.ClashStart -> {
                            design.fetch()
                            refreshProxy()
                            if (activeRoute.value == ROUTE_CONNECTIONS) refreshConnections()
                        }
                        Event.ClashStop -> {
                            design.fetch()
                            testedProxyGroups.clear()
                            testedActiveProxies.clear()
                            proxyUiState.value = ProxyUiState(serviceRunning = false)
                            connectionsUiState.value = ConnectionsUiState()
                            homeUiState.value = homeUiState.value.copy(
                                uploadSpeed = "-- B/s",
                                downloadSpeed = "-- B/s",
                            )
                        }
                        Event.ProfileLoaded -> {
                            design.fetch()
                            testedProxyGroups.clear()
                            testedActiveProxies.clear()
                            refreshProxy()
                        }
                        Event.ProfileUpdateCompleted, Event.ProfileUpdateFailed -> profilesViewModel.refresh()
                        else -> Unit
                    }
                }
                design.routePreviewRequests.onReceive { target ->
                    try {
                        design.setRoutePreview(withClash { queryRoutePreview(target) })
                    } catch (_: Exception) {
                        design.setRoutePreviewError()
                    }
                }
                if (clashRunning) ticker.onReceive {
                    design.fetchTraffic()
                    if (activeRoute.value == ROUTE_CONNECTIONS) refreshConnections()
                }
            }
        }
    }

    @Composable
    private fun AuroraApp(design: MainDesign) {
        val navController = rememberNavController()
        val startDestination = remember {
            uiStore.lastMainRoute.takeIf { it in PERSISTED_MAIN_ROUTES } ?: ROUTE_HOME
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

        LaunchedEffect(requestedRoute.value, backStackEntry) {
            val route = requestedRoute.value
            if (route != null && backStackEntry != null) {
                if (navController.currentDestination?.route != route) {
                    navController.navigateTopLevel(route)
                }
                requestedRoute.value = null
            }
        }

        LaunchedEffect(currentRoute, homeUiState.value.running) {
            activeRoute.value = currentRoute
            if (currentRoute in PERSISTED_MAIN_ROUTES) {
                uiStore.lastMainRoute = currentRoute
            }
            if (currentRoute == ROUTE_CONNECTIONS) refreshConnections()
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                AuroraBottomNavigation(
                    selected = currentRoute.toAuroraDestination(),
                    proxyEnabled = homeUiState.value.running,
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
                    HomeScreen(
                        state = homeUiState.value,
                        onToggleConnection = {
                            launch { if (clashRunning) stopClashService() else design.startClash() }
                        },
                        onModeSelected = { launch { design.patchMode(it) } },
                        onTestLatency = {
                            if (latencyTestJob?.isActive != true) {
                                latencyTestJob = launch { design.testSiteLatency() }
                            }
                        },
                        onOpenConnections = { navController.navigateTopLevel(ROUTE_CONNECTIONS) },
                        onOpenLogs = {
                            if (LogcatService.running) startActivity(LogcatActivity::class.intent)
                            else startActivity(LogsActivity::class.intent)
                        },
                        onOpenRouteTest = design::showRouteTest,
                        onOpenDns = { startActivity(NetworkSettingsActivity::class.intent) },
                        onOpenProfiles = { navController.navigate(ROUTE_PROFILES) },
                        onOpenProxy = { navController.navigateTopLevel(ROUTE_PROXY) },
                        onOpenSettings = { navController.navigateTopLevel(ROUTE_SETTINGS) },
                        showBottomNavigation = false,
                    )
                }
                composable(ROUTE_PROXY) {
                    ProxyScreen(
                        state = proxyUiState.value,
                        onSelectGroup = ::selectProxyGroup,
                        onSelectProxy = { index, name -> launch { selectProxy(index, name) } },
                        onTestGroup = { launch { testProxyGroup(it) } },
                        onRefresh = ::refreshActiveProxyEndpoints,
                        onSortChanged = { sort ->
                            uiStore.proxySort = sort
                            refreshProxy()
                        },
                        onHideUnselectableChanged = { hide ->
                            uiStore.proxyExcludeNotSelectable = hide
                            refreshProxy()
                        },
                    )
                }
                composable(ROUTE_PROFILES) {
                    val state by profilesViewModel.uiState.collectAsStateWithLifecycle()
                    ProfilesScreen(
                        state = state,
                        onImportFile = { launch { importProfileFromFile() } },
                        onImportUrl = { name, url -> launch { importProfile(Profile.Type.Url, name, url) } },
                        onScanQrCode = { scanLauncher.launch(null) },
                        onActivateProfile = profilesViewModel::activate,
                        onUpdateProfile = profilesViewModel::update,
                        onEditProfile = { startActivity(PropertiesActivity::class.intent.setUUID(it)) },
                        onDuplicateProfile = { launch { duplicateProfile(it) } },
                        onDeleteProfile = profilesViewModel::delete,
                        onUpdateAll = profilesViewModel::updateAll,
                        proxyEnabled = homeUiState.value.running,
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
                        state = connectionsUiState.value,
                        onRefresh = { launch { refreshConnections() } },
                        onCloseConnection = { id ->
                            launch {
                                runCatching { withClash { closeConnection(id) } }
                                    .onFailure(::showError)
                                refreshConnections()
                            }
                        },
                        onCloseAll = {
                            launch {
                                runCatching { withClash { closeAllConnections() } }
                                    .onFailure(::showError)
                                refreshConnections()
                            }
                        },
                    )
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        proxyEnabled = homeUiState.value.running,
                        onOpenHome = { navController.navigateTopLevel(ROUTE_HOME) },
                        onOpenProxy = { navController.navigateTopLevel(ROUTE_PROXY) },
                        onOpenConnections = { navController.navigateTopLevel(ROUTE_CONNECTIONS) },
                        onOpenProfiles = { navController.navigate(ROUTE_PROFILES) },
                        onOpenNetwork = { startActivity(NetworkSettingsActivity::class.intent) },
                        onOpenApp = { startActivity(AppSettingsActivity::class.intent) },
                        onOpenMetaFeature = { startActivity(MetaFeatureSettingsActivity::class.intent) },
                        onOpenOverride = { startActivity(OverrideSettingsActivity::class.intent) },
                        showBottomNavigation = false,
                    )
                }
            }
        }
    }

    private fun NavHostController.navigateTopLevel(route: String) {
        navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.startDestinationId) { saveState = true }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(clashRunning)
        val state = withClash { queryTunnelState() }
        val providers = withClash { queryProviders() }
        val profileName = withProfile { queryActive()?.name }

        setMode(state.mode)
        setHasProviders(providers.isNotEmpty())
        setProfileName(profileName)
        homeUiState.value = homeUiState.value.copy(
            running = clashRunning,
            mode = state.mode,
            profileName = profileName,
        )
    }

    private suspend fun MainDesign.fetchTraffic() {
        withClash {
            setForwarded(queryTrafficTotal())
            val traffic = queryTrafficNow()
            homeUiState.value = homeUiState.value.copy(
                uploadSpeed = "${traffic.trafficUpload()}/s",
                downloadSpeed = "${traffic.trafficDownload()}/s",
            )
        }
    }

    private suspend fun MainDesign.patchMode(mode: TunnelState.Mode) {
        withClash {
            val override = queryOverride(Clash.OverrideSlot.Session)
            override.mode = mode
            patchOverride(Clash.OverrideSlot.Session, override)
        }
        setMode(mode)
        homeUiState.value = homeUiState.value.copy(mode = mode)
    }

    private fun refreshProxy() {
        proxyRefreshJob?.cancel()
        proxyRefreshJob = launch {
            if (!clashRunning) {
                proxyUiState.value = ProxyUiState(serviceRunning = false)
                return@launch
            }
            proxyUiState.value = proxyUiState.value.copy(
                loading = true,
                serviceRunning = true,
                errorMessage = null,
            )
            runCatching {
                val names = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
                val groupsByName = linkedMapOf<String, ProxyGroup>()
                val rootGroups = coroutineScope {
                    names.map { name ->
                        async {
                            name to withClash { queryProxyGroup(name, uiStore.proxySort) }
                        }
                    }.awaitAll()
                }
                groupsByName.putAll(rootGroups)

                suspend fun resolveRoute(proxy: Proxy, visited: Set<String> = emptySet()): ProxyRouteUiState? {
                    if (!proxy.isGroup || proxy.name in visited) return null
                    val group = groupsByName[proxy.name] ?: withClash {
                        queryProxyGroup(proxy.name, uiStore.proxySort)
                    }.also { groupsByName[proxy.name] = it }
                    val selected = group.proxies.firstOrNull { it.name == group.now } ?: return null
                    val nextVisited = visited + proxy.name
                    val childRoute = if (selected.isGroup) resolveRoute(selected, nextVisited) else null
                    return ProxyRouteUiState(
                        names = listOf(proxy.name) + (childRoute?.names ?: listOf(selected.name)),
                        delay = childRoute?.delay ?: selected.delay,
                    )
                }

                val groups = names.map { name ->
                    val group = groupsByName.getValue(name)
                    val routes = group.proxies.mapNotNull { proxy ->
                        resolveRoute(proxy)?.let { proxy.name to it }
                    }.toMap()
                    ProxyGroupUiState(
                        name = name,
                        type = group.type,
                        selectedProxy = group.now,
                        selectable = group.type == "Selector",
                        delayTested = name in testedProxyGroups,
                        proxies = group.proxies,
                        nestedRoutes = routes,
                        activeDelay = routes[group.now]?.delay
                            ?: group.proxies.firstOrNull { it.name == group.now }?.delay
                            ?: 65535,
                        activeDelayTested = activeEndpointFor(group.now, routes, group.proxies)
                            ?.let { it in testedActiveProxies } == true,
                    )
                }
                val selected = names.indexOf(uiStore.proxyLastGroup).takeIf { it >= 0 } ?: 0
                ProxyUiState(
                    loading = false,
                    serviceRunning = true,
                    groups = groups,
                    selectedGroupIndex = selected,
                    sort = uiStore.proxySort,
                    hideUnselectableGroups = uiStore.proxyExcludeNotSelectable,
                    activeEndpointsTesting = proxyUiState.value.activeEndpointsTesting,
                )
            }.onSuccess { proxyUiState.value = it }
                .onFailure {
                    proxyUiState.value = proxyUiState.value.copy(
                        loading = false,
                        serviceRunning = clashRunning,
                        errorMessage = it.message ?: "代理列表加载失败",
                    )
                }
        }
    }

    private suspend fun refreshConnections() {
        if (!clashRunning) {
            connectionsUiState.value = ConnectionsUiState()
            return
        }
        val current = connectionsUiState.value
        connectionsUiState.value = current.copy(
            serviceRunning = true,
            loading = current.connections.isEmpty(),
            errorMessage = null,
        )
        runCatching { withClash { queryConnections().toList() } }
            .onSuccess { connections ->
                connectionsUiState.value = ConnectionsUiState(
                    serviceRunning = true,
                    connections = connections,
                )
            }
            .onFailure { error ->
                connectionsUiState.value = connectionsUiState.value.copy(
                    serviceRunning = true,
                    loading = false,
                    errorMessage = error.message ?: "连接读取失败",
                )
            }
    }

    private fun selectProxyGroup(index: Int) {
        val group = proxyUiState.value.groups.getOrNull(index) ?: return
        uiStore.proxyLastGroup = group.name
        proxyUiState.value = proxyUiState.value.copy(selectedGroupIndex = index)
    }

    private fun activeEndpointFor(
        selected: String,
        routes: Map<String, ProxyRouteUiState>,
        proxies: List<Proxy>,
    ): String? {
        val route = routes[selected]?.names?.lastOrNull()
        if (!route.isNullOrBlank()) return route
        return proxies.firstOrNull { it.name == selected && !it.isGroup }?.name
    }

    private fun refreshActiveProxyEndpoints() {
        if (proxyUiState.value.activeEndpointsTesting) return
        if (!clashRunning) {
            refreshProxy()
            return
        }
        launch {
            val currentGroups = proxyUiState.value.groups
            val endpoints = currentGroups.mapNotNull { group ->
                activeEndpointFor(group.selectedProxy, group.nestedRoutes, group.proxies)
            }.distinct()
            if (endpoints.isEmpty()) {
                refreshProxy()
                return@launch
            }

            proxyUiState.value = proxyUiState.value.copy(activeEndpointsTesting = true)
            try {
                endpoints.forEach { endpoint -> withClash { testProxy(endpoint) } }
                testedActiveProxies += endpoints
            } catch (error: Exception) {
                showError(error)
            } finally {
                proxyUiState.value = proxyUiState.value.copy(activeEndpointsTesting = false)
                refreshProxy()
            }
        }
    }

    private suspend fun selectProxy(index: Int, name: String) {
        val group = proxyUiState.value.groups.getOrNull(index) ?: return
        if (!group.selectable) return
        runCatching { withClash { patchSelector(group.name, name) } }
            .onSuccess {
                val groups = proxyUiState.value.groups.toMutableList()
                groups[index] = group.copy(selectedProxy = name)
                proxyUiState.value = proxyUiState.value.copy(groups = groups)
                refreshProxy()
            }
            .onFailure(::showError)
    }

    private suspend fun testProxyGroup(index: Int) {
        val group = proxyUiState.value.groups.getOrNull(index) ?: return
        setProxyGroupTesting(index, true)
        try {
            withClash { healthCheck(group.name) }
            val refreshed = withClash { queryProxyGroup(group.name, uiStore.proxySort) }
            val groups = proxyUiState.value.groups.toMutableList()
            testedProxyGroups += group.name
            groups[index] = group.copy(
                selectedProxy = refreshed.now,
                selectable = refreshed.type == "Selector",
                delayTested = true,
                proxies = refreshed.proxies,
                testing = false,
            )
            proxyUiState.value = proxyUiState.value.copy(groups = groups)
            refreshProxy()
        } catch (error: Exception) {
            setProxyGroupTesting(index, false)
            showError(error)
        }
    }

    private fun setProxyGroupTesting(index: Int, testing: Boolean) {
        val groups = proxyUiState.value.groups.toMutableList()
        val group = groups.getOrNull(index) ?: return
        groups[index] = group.copy(testing = testing)
        proxyUiState.value = proxyUiState.value.copy(groups = groups)
    }

    private suspend fun MainDesign.testSiteLatency() {
        val targets = listOf(
            MainDesign.LatencySite.Apple to "https://www.apple.com/library/test/success.html",
            MainDesign.LatencySite.GitHub to "https://github.com/",
            MainDesign.LatencySite.YouTube to "https://www.youtube.com/generate_204",
            MainDesign.LatencySite.Google to "https://www.google.com/generate_204",
        )
        setLatencyTesting(true)
        homeUiState.value = homeUiState.value.copy(
            latencyTesting = true,
            appleLatency = "检测中",
            githubLatency = "检测中",
            youtubeLatency = "检测中",
            googleLatency = "检测中",
        )
        try {
            coroutineScope {
                targets.map { (site, url) -> async(Dispatchers.IO) { site to measureHttpLatency(url) } }
                    .awaitAll()
            }.forEach { (site, latency) ->
                setSiteLatency(site, latency)
                val value = latency?.let { "$it ms" } ?: "超时"
                homeUiState.value = when (site) {
                    MainDesign.LatencySite.Apple -> homeUiState.value.copy(appleLatency = value)
                    MainDesign.LatencySite.GitHub -> homeUiState.value.copy(githubLatency = value)
                    MainDesign.LatencySite.YouTube -> homeUiState.value.copy(youtubeLatency = value)
                    MainDesign.LatencySite.Google -> homeUiState.value.copy(googleLatency = value)
                }
            }
        } finally {
            setLatencyTesting(false)
            homeUiState.value = homeUiState.value.copy(latencyTesting = false)
        }
    }

    private fun measureHttpLatency(url: String): Long? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 6_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            useCaches = false
            setRequestProperty("User-Agent", "Aurora connectivity check")
        }
        return try {
            val startedAt = SystemClock.elapsedRealtime()
            connection.connect()
            connection.responseCode
            SystemClock.elapsedRealtime() - startedAt
        } catch (_: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun MainDesign.startClash() {
        val active = withProfile { queryActive() }
        if (active == null || !active.imported) {
            Toast.makeText(this@MainActivity, DesignR.string.no_profile_selected, Toast.LENGTH_LONG).show()
            requestedRoute.value = ROUTE_PROFILES
            return
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
        importProfile(Profile.Type.File, name) { uuid ->
            FilesClient(this).copyDocument("$uuid/config.yaml", uri)
        }
    }

    private suspend fun importProfile(
        type: Profile.Type,
        name: String,
        source: String = "",
        prepare: suspend (UUID) -> Unit = {},
    ) {
        var uuid: UUID? = null
        try {
            uuid = withProfile { create(type, name, source) }
            prepare(uuid)
            withProfile { commit(uuid) { } }
            profilesViewModel.refresh()
        } catch (error: Exception) {
            uuid?.let { failedId -> runCatching { withProfile { delete(failedId) } } }
            showError(error)
            profilesViewModel.refresh()
        }
    }

    private suspend fun duplicateProfile(uuid: UUID) {
        runCatching { withProfile { clone(uuid) } }
            .onSuccess {
                startActivity(PropertiesActivity::class.intent.setUUID(it))
                profilesViewModel.refresh()
            }
            .onFailure(::showError)
    }

    private fun scanResultHandler(result: QRResult) {
        launch {
            when (result) {
                is QRResult.QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.let(::String).orEmpty()
                    if (url.isNotBlank()) importProfile(
                        Profile.Type.Url,
                        getString(DesignR.string.new_profile),
                        url,
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
    }

    private fun showPlaceholder() {
        Toast.makeText(this, DesignR.string.aurora_feature_placeholder, Toast.LENGTH_SHORT).show()
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this, error.message ?: "操作失败", Toast.LENGTH_LONG).show()
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

    private fun routeFromIntent(intent: Intent): String =
        intent.getStringExtra(EXTRA_TOP_LEVEL_ROUTE)
            ?.takeIf { it in TOP_LEVEL_ROUTES }
            ?: if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES ||
                intent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES"
            ) ROUTE_SETTINGS else ROUTE_HOME

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
        private val TOP_LEVEL_ROUTES = setOf(ROUTE_HOME, ROUTE_PROXY, ROUTE_CONNECTIONS, ROUTE_PROFILES, ROUTE_SETTINGS)
        private val PERSISTED_MAIN_ROUTES = setOf(ROUTE_HOME, ROUTE_PROXY, ROUTE_CONNECTIONS, ROUTE_SETTINGS)
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
    else -> AuroraDestination.Home
}
