package com.koude.aurora.ui.proxy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.ProxySort
import com.koude.aurora.data.proxy.ProxyPreferences
import com.koude.aurora.data.proxy.ProxyRepository
import com.koude.aurora.data.proxy.ServiceProxyRepository
import com.koude.aurora.data.proxy.StoredProxyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProxyViewModel(
    private val repository: ProxyRepository,
    private val preferences: ProxyPreferences,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = mutableUiState.asStateFlow()
    private val errorChannel = Channel<Throwable>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    private val testedGroups = mutableSetOf<String>()
    private val testedEndpoints = mutableSetOf<String>()
    private var refreshJob: Job? = null
    private var refreshGeneration = 0
    private var operationGeneration = 0
    private var serviceRunning = false

    fun refresh(running: Boolean) {
        serviceRunning = running
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        if (!running) {
            mutableUiState.value = ProxyUiState(serviceRunning = false)
            return
        }
        mutableUiState.update { it.copy(loading = true, serviceRunning = true, errorMessage = null) }
        refreshJob = viewModelScope.launch {
            try {
                val sort = preferences.sort
                val hideUnselectable = preferences.hideUnselectable
                val names = repository.groupNames(hideUnselectable)
                val groupsByName = linkedMapOf<String, ProxyGroup>()
                val roots = coroutineScope {
                    names.map { name -> async { name to repository.group(name, sort) } }.awaitAll()
                }
                groupsByName.putAll(roots)

                suspend fun resolveRoute(proxy: Proxy, visited: Set<String> = emptySet()): ProxyRouteUiState? {
                    if (!proxy.isGroup || proxy.name in visited) return null
                    val group = groupsByName[proxy.name] ?: repository.group(proxy.name, sort)
                        .also { groupsByName[proxy.name] = it }
                    val selected = group.proxies.firstOrNull { it.name == group.now } ?: return null
                    val childRoute = if (selected.isGroup) resolveRoute(selected, visited + proxy.name) else null
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
                        delayTested = name in testedGroups,
                        proxies = group.proxies,
                        nestedRoutes = routes,
                        activeDelay = routes[group.now]?.delay
                            ?: group.proxies.firstOrNull { it.name == group.now }?.delay
                            ?: 65535,
                        activeDelayTested = activeEndpointFor(group.now, routes, group.proxies)
                            ?.let { it in testedEndpoints } == true,
                    )
                }
                if (generation != refreshGeneration || !serviceRunning) return@launch
                val selected = names.indexOf(preferences.lastGroup).takeIf { it >= 0 } ?: 0
                mutableUiState.update { previous ->
                    ProxyUiState(
                        serviceRunning = true,
                        groups = groups,
                        selectedGroupIndex = selected,
                        expandedGroups = previous.expandedGroups,
                        sort = sort,
                        hideUnselectableGroups = hideUnselectable,
                        activeEndpointsTesting = previous.activeEndpointsTesting,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation != refreshGeneration || !serviceRunning) return@launch
                mutableUiState.update {
                    it.copy(loading = false, errorMessage = error.message ?: "代理列表加载失败")
                }
            }
        }
    }

    fun onServiceStopped() {
        operationGeneration++
        testedGroups.clear()
        testedEndpoints.clear()
        refresh(false)
    }

    fun onProfileLoaded(running: Boolean) {
        operationGeneration++
        testedGroups.clear()
        testedEndpoints.clear()
        refresh(running)
    }

    fun onRouteChanged(route: String, proxyRoute: String) {
        mutableUiState.update { state ->
            val expanded = expandedProxyGroupsForRoute(route, proxyRoute, state.expandedGroups)
            if (expanded == state.expandedGroups) state else state.copy(expandedGroups = expanded)
        }
    }

    fun selectGroup(index: Int) {
        val group = uiState.value.groups.getOrNull(index) ?: return
        preferences.lastGroup = group.name
        mutableUiState.update { it.copy(selectedGroupIndex = index) }
    }

    fun setGroupExpanded(name: String, expanded: Boolean) {
        mutableUiState.update { it.copy(expandedGroups = expandedProxyGroup(name, expanded)) }
    }

    fun setSort(sort: ProxySort) {
        preferences.sort = sort
        refresh(serviceRunning)
    }

    fun setHideUnselectable(hide: Boolean) {
        preferences.hideUnselectable = hide
        refresh(serviceRunning)
    }

    fun testActiveEndpoints() {
        if (uiState.value.activeEndpointsTesting) return
        if (!serviceRunning) {
            refresh(false)
            return
        }
        val generation = operationGeneration
        viewModelScope.launch {
            val endpoints = uiState.value.groups.mapNotNull { group ->
                activeEndpointFor(group.selectedProxy, group.nestedRoutes, group.proxies)
            }.distinct()
            if (endpoints.isEmpty()) {
                refresh(serviceRunning)
                return@launch
            }
            mutableUiState.update { it.copy(activeEndpointsTesting = true) }
            try {
                endpoints.forEach { repository.test(it) }
                if (generation == operationGeneration && serviceRunning) testedEndpoints += endpoints
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == operationGeneration) errorChannel.send(error)
            } finally {
                if (generation == operationGeneration) {
                    mutableUiState.update { it.copy(activeEndpointsTesting = false) }
                    refresh(serviceRunning)
                }
            }
        }
    }

    fun selectProxy(index: Int, name: String) {
        val group = uiState.value.groups.getOrNull(index) ?: return
        if (!group.selectable || group.selectingProxy != null) return
        updateGroup(index) { it.copy(selectingProxy = name) }
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                repository.select(group.name, name)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == operationGeneration) {
                    errorChannel.send(IllegalStateException("切换到 $name 失败：${error.message ?: "操作未成功"}", error))
                }
            } finally {
                if (generation == operationGeneration) {
                    updateGroup(index) { if (it.selectingProxy == name) it.copy(selectingProxy = null) else it }
                    refresh(serviceRunning)
                }
            }
        }
    }

    fun testGroup(index: Int) {
        val group = uiState.value.groups.getOrNull(index) ?: return
        updateGroup(index) { it.copy(testing = true) }
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                repository.healthCheck(group.name)
                val refreshed = repository.group(group.name, preferences.sort)
                if (generation != operationGeneration || !serviceRunning) return@launch
                testedGroups += group.name
                updateGroup(index) {
                    it.copy(
                        selectedProxy = refreshed.now,
                        selectable = refreshed.type == "Selector",
                        delayTested = true,
                        proxies = refreshed.proxies,
                        testing = false,
                    )
                }
                refresh(serviceRunning)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == operationGeneration) {
                    updateGroup(index) { it.copy(testing = false) }
                    errorChannel.send(error)
                }
            }
        }
    }

    private fun updateGroup(index: Int, transform: (ProxyGroupUiState) -> ProxyGroupUiState) {
        mutableUiState.update { state ->
            val groups = state.groups.toMutableList()
            val group = groups.getOrNull(index) ?: return@update state
            groups[index] = transform(group)
            state.copy(groups = groups)
        }
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

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProxyViewModel(ServiceProxyRepository(), StoredProxyPreferences(context.applicationContext)) as T
        }
    }
}
