package com.github.kr328.clash

import android.Manifest.permission.INTERNET
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.getSystemService
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.AccessControlScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessControlActivity : BaseActivity() {
    private val mode = mutableStateOf(AccessControlMode.DenySelected)
    private val selectedPackages = mutableStateOf<Set<String>>(emptySet())
    private val appList = mutableStateOf<List<AppInfo>>(emptyList())

    override suspend fun main() {
        val service = ServiceStore(this)
        val originalMode = service.accessControlMode
        val originalPackages = withContext(Dispatchers.IO) { service.accessControlPackages.toSet() }
        // The legacy "all apps" mode is equivalent to an empty exclusion list.
        mode.value = if (originalMode == AccessControlMode.AcceptAll) AccessControlMode.DenySelected else originalMode
        selectedPackages.value = if (originalMode == AccessControlMode.AcceptAll) emptySet() else originalPackages
        val initialMode = mode.value
        val initialPackages = selectedPackages.value
        appList.value = loadApps(selectedPackages.value)

        defer {
            withContext(Dispatchers.IO) {
                val selected = selectedPackages.value
                val selectedMode = mode.value
                val changed = selectedMode != initialMode || selected != initialPackages
                if (!changed) return@withContext
                val behaviorChanged = !(originalMode == AccessControlMode.AcceptAll &&
                    selectedMode == AccessControlMode.DenySelected && selected.isEmpty())
                service.accessControlMode = selectedMode
                service.accessControlPackages = selected
                if (clashRunning && behaviorChanged) {
                    stopClashService()
                    while (clashRunning) delay(200)
                    startClashService()
                }
            }
        }

        setContent {
            AuroraTheme {
                AccessControlScreen(
                    apps = appList.value,
                    mode = mode.value,
                    selectedPackages = selectedPackages.value,
                    showSystemApps = uiStore.accessControlSystemApp,
                    sort = uiStore.accessControlSort,
                    reverse = uiStore.accessControlReverse,
                    onModeChanged = { mode.value = it },
                    onToggleApp = { packageName ->
                        selectedPackages.value = selectedPackages.value.toMutableSet().apply {
                            if (!add(packageName)) remove(packageName)
                        }
                    },
                    onSelectAll = { selectedPackages.value = appList.value.map(AppInfo::packageName).toSet() },
                    onSelectNone = { selectedPackages.value = emptySet() },
                    onSelectInvert = { selectedPackages.value = appList.value.map(AppInfo::packageName).toSet() - selectedPackages.value },
                    onShowSystemAppsChanged = { show ->
                        uiStore.accessControlSystemApp = show
                        launch { appList.value = loadApps(selectedPackages.value) }
                    },
                    onSortChanged = { value ->
                        uiStore.accessControlSort = value
                        launch { appList.value = loadApps(selectedPackages.value) }
                    },
                    onReverseChanged = { value ->
                        uiStore.accessControlReverse = value
                        launch { appList.value = loadApps(selectedPackages.value) }
                    },
                    onImport = ::importPackages,
                    onExport = ::exportPackages,
                    onBack = ::finish,
                )
            }
        }
    }

    private fun importPackages() {
        val data = getSystemService<ClipboardManager>()?.primaryClip ?: return
        if (data.itemCount == 0) return
        val names = data.getItemAt(0).text?.split('\n')?.toSet().orEmpty()
        selectedPackages.value = appList.value.map(AppInfo::packageName).intersect(names)
    }

    private fun exportPackages() {
        getSystemService<ClipboardManager>()?.setPrimaryClip(
            ClipData.newPlainText("packages", selectedPackages.value.joinToString("\n")),
        )
    }

    private suspend fun loadApps(selected: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val reverse = uiStore.accessControlReverse
        val sort = uiStore.accessControlSort
        val includeSystemApps = uiStore.accessControlSystemApp
        val selectedFirst = compareByDescending<AppInfo> { it.packageName in selected }
        val comparator = if (reverse) selectedFirst.thenDescending(sort) else selectedFirst.then(sort)
        val manager = packageManager

        manager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .asSequence()
            .filter { it.packageName != packageName }
            .filter { it.applicationInfo != null }
            .filter {
                it.requestedPermissions?.contains(INTERNET) == true ||
                    it.applicationInfo!!.uid < android.os.Process.FIRST_APPLICATION_UID
            }
            .filter { includeSystemApps || !it.isSystemApp }
            .map { it.toAppInfo(manager) }
            .sortedWith(comparator)
            .toList()
    }

    private val PackageInfo.isSystemApp: Boolean
        get() = applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
}
