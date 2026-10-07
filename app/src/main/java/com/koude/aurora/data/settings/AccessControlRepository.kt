package com.koude.aurora.data.settings

import android.Manifest.permission.INTERNET
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AccessControlSelection(val mode: AccessControlMode, val packages: Set<String>)
data class AccessControlOptions(
    val showSystemApps: Boolean,
    val sort: AppInfoSort,
    val reverse: Boolean,
    val vpnEnabled: Boolean,
)

interface AccessControlRepository {
    suspend fun selection(): AccessControlSelection
    fun options(): AccessControlOptions
    fun setShowSystemApps(value: Boolean)
    fun setSort(value: AppInfoSort)
    fun setReverse(value: Boolean)
    suspend fun apps(selected: Set<String>): List<AppInfo>
    suspend fun save(requested: AccessControlSelection): Boolean
}

class StoredAccessControlRepository(context: Context) : AccessControlRepository {
    private val appContext = context.applicationContext
    private val service = ServiceStore(appContext)
    private val ui = UiStore(appContext)

    override suspend fun selection(): AccessControlSelection = withContext(Dispatchers.IO) {
        AccessControlSelection(service.accessControlMode, service.accessControlPackages.toSet())
    }

    override fun options() = AccessControlOptions(
        ui.accessControlSystemApp, ui.accessControlSort, ui.accessControlReverse, ui.enableVpn,
    )

    override fun setShowSystemApps(value: Boolean) { ui.accessControlSystemApp = value }
    override fun setSort(value: AppInfoSort) { ui.accessControlSort = value }
    override fun setReverse(value: Boolean) { ui.accessControlReverse = value }

    override suspend fun apps(selected: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val options = options()
        val selectedFirst = compareByDescending<AppInfo> { it.packageName in selected }
        val comparator = if (options.reverse) selectedFirst.thenDescending(options.sort)
            else selectedFirst.then(options.sort)
        val manager = appContext.packageManager

        manager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .asSequence()
            .filter { it.packageName != appContext.packageName }
            .filter { it.applicationInfo != null }
            .filter {
                it.requestedPermissions?.contains(INTERNET) == true ||
                    it.applicationInfo!!.uid < android.os.Process.FIRST_APPLICATION_UID
            }
            .filter { options.showSystemApps || !it.isSystemApp }
            .map { it.toAppInfo(manager) }
            .sortedWith(comparator)
            .toList()
    }

    override suspend fun save(requested: AccessControlSelection): Boolean = withContext(Dispatchers.IO) {
        val previous = AccessControlSelection(service.accessControlMode, service.accessControlPackages.toSet())
        val behaviorChanged = previous != requested
        service.accessControlMode = requested.mode
        service.accessControlPackages = requested.packages
        behaviorChanged && !(previous.mode == AccessControlMode.AcceptAll &&
            requested.mode == AccessControlMode.DenySelected && requested.packages.isEmpty())
    }

    private val PackageInfo.isSystemApp: Boolean
        get() = applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
}
