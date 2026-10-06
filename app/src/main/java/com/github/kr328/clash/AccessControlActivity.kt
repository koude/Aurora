package com.github.kr328.clash

import android.Manifest.permission.INTERNET
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.widget.Toast
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AccessControlActivity : BaseActivity() {
    private val mode = mutableStateOf(AccessControlMode.DenySelected)
    private val selectedPackages = mutableStateOf<Set<String>>(emptySet())
    private val appList = mutableStateOf<List<AppInfo>>(emptyList())
    private val applying = mutableStateOf(false)

    override suspend fun main() {
        val service = ServiceStore(this)
        val originalMode = service.accessControlMode
        val originalPackages = withContext(Dispatchers.IO) { service.accessControlPackages.toSet() }
        // The legacy "all apps" mode is equivalent to an empty exclusion list.
        mode.value = if (originalMode == AccessControlMode.AcceptAll) AccessControlMode.DenySelected else originalMode
        selectedPackages.value = if (originalMode == AccessControlMode.AcceptAll) emptySet() else originalPackages
        val savedMode = mutableStateOf(mode.value)
        val savedPackages = mutableStateOf(selectedPackages.value)
        appList.value = loadApps(selectedPackages.value)

        setContent {
            AuroraTheme {
                AccessControlScreen(
                    apps = appList.value,
                    mode = mode.value,
                    selectedPackages = selectedPackages.value,
                    hasUnsavedChanges = mode.value != savedMode.value || selectedPackages.value != savedPackages.value,
                    applying = applying.value,
                    showSystemApps = uiStore.accessControlSystemApp,
                    sort = uiStore.accessControlSort,
                    reverse = uiStore.accessControlReverse,
                    onModeChanged = { if (!applying.value) mode.value = it },
                    onToggleApp = { packageName ->
                        if (!applying.value) {
                            selectedPackages.value = selectedPackages.value.toMutableSet().apply {
                                if (!add(packageName)) remove(packageName)
                            }
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
                    onApply = {
                        if (!applying.value && (mode.value != AccessControlMode.AcceptSelected || selectedPackages.value.isNotEmpty())) {
                            val requestedMode = mode.value
                            val requestedPackages = selectedPackages.value.toSet()
                            val applyToRunningVpn = clashRunning && uiStore.enableVpn
                            applying.value = true
                            launch {
                                var stored = false
                                var failure: Throwable? = null
                                try {
                                    withContext(NonCancellable + Dispatchers.IO) {
                                        val previousMode = service.accessControlMode
                                        val previousPackages = service.accessControlPackages.toSet()
                                        val behaviorChanged = previousMode != requestedMode || previousPackages != requestedPackages
                                        service.accessControlMode = requestedMode
                                        service.accessControlPackages = requestedPackages
                                        stored = true
                                        if (applyToRunningVpn && behaviorChanged &&
                                            !(previousMode == AccessControlMode.AcceptAll &&
                                                requestedMode == AccessControlMode.DenySelected && requestedPackages.isEmpty())) {
                                            stopClashService()
                                            val stopped = withTimeoutOrNull(15_000) {
                                                while (clashRunning) delay(200)
                                                true
                                            } == true
                                            if (!stopped) error("VPN stop timed out")
                                            if (startClashService() != null) error("VPN permission required")
                                        }
                                    }
                                } catch (error: Exception) {
                                    failure = error
                                } finally {
                                    if (stored) {
                                        savedMode.value = requestedMode
                                        savedPackages.value = requestedPackages
                                        try {
                                            appList.value = withContext(NonCancellable) { loadApps(requestedPackages) }
                                        } catch (_: Exception) {
                                            // The saved selection is still valid if refreshing the list fails.
                                        }
                                    }
                                    applying.value = false
                                    Toast.makeText(
                                        this@AccessControlActivity,
                                        if (failure == null && applyToRunningVpn) "已保存并应用" else if (failure == null)
                                            "已保存，启动 VPN 服务后生效" else if (stored)
                                            "名单已保存，代理重启失败，请手动重新连接" else "保存失败，请重试",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                        }
                    },
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
