package com.koude.aurora.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.service.model.AccessControlMode
import com.koude.aurora.data.settings.AccessControlOptions
import com.koude.aurora.data.settings.AccessControlRepository
import com.koude.aurora.data.settings.AccessControlSelection
import com.koude.aurora.data.settings.StoredAccessControlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class AccessControlViewModel(private val repository: AccessControlRepository) : ViewModel() {
    var mode by mutableStateOf(AccessControlMode.DenySelected)
        private set
    var selectedPackages by mutableStateOf<Set<String>>(emptySet())
        private set
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var options by mutableStateOf<AccessControlOptions?>(null)
        private set
    var applying by mutableStateOf(false)
        private set
    private var saved = AccessControlSelection(AccessControlMode.DenySelected, emptySet())
    val hasUnsavedChanges: Boolean get() = mode != saved.mode || selectedPackages != saved.packages

    suspend fun load() {
        val original = repository.selection()
        mode = if (original.mode == AccessControlMode.AcceptAll) AccessControlMode.DenySelected else original.mode
        selectedPackages = if (original.mode == AccessControlMode.AcceptAll) emptySet() else original.packages
        saved = AccessControlSelection(mode, selectedPackages)
        options = repository.options()
        refreshApps()
    }

    fun changeMode(value: AccessControlMode) { if (!applying) mode = value }
    fun toggle(packageName: String) {
        if (!applying) selectedPackages = selectedPackages.toMutableSet().apply {
            if (!add(packageName)) remove(packageName)
        }
    }
    fun selectAll() { selectedPackages = apps.map(AppInfo::packageName).toSet() }
    fun selectNone() { selectedPackages = emptySet() }
    fun selectInvert() { selectedPackages = apps.map(AppInfo::packageName).toSet() - selectedPackages }
    fun import(names: Set<String>) { selectedPackages = apps.map(AppInfo::packageName).intersect(names) }
    fun export(): String = selectedPackages.joinToString("\n")

    suspend fun setShowSystemApps(value: Boolean) {
        repository.setShowSystemApps(value)
        options = repository.options()
        refreshApps()
    }
    suspend fun setSort(value: AppInfoSort) {
        repository.setSort(value)
        options = repository.options()
        refreshApps()
    }
    suspend fun setReverse(value: Boolean) {
        repository.setReverse(value)
        options = repository.options()
        refreshApps()
    }
    suspend fun refreshApps() { apps = repository.apps(selectedPackages) }

    fun beginApply(): Boolean {
        if (applying || (mode == AccessControlMode.AcceptSelected && selectedPackages.isEmpty())) return false
        applying = true
        return true
    }

    suspend fun apply(runningVpn: Boolean, restartVpn: suspend () -> Unit): String? {
        if (!applying) return null
        val requested = AccessControlSelection(mode, selectedPackages.toSet())
        var stored = false
        var failure: Throwable? = null
        try {
            withContext(NonCancellable + Dispatchers.IO) {
                val restartNeeded = repository.save(requested)
                stored = true
                if (runningVpn && restartNeeded) restartVpn()
            }
        } catch (error: Exception) {
            failure = error
        } finally {
            if (stored) {
                saved = requested
                try { withContext(NonCancellable) { refreshApps() } }
                catch (_: Exception) { /* Saved selection remains valid if listing fails. */ }
            }
            applying = false
        }
        return if (failure == null && runningVpn) "已保存并应用"
            else if (failure == null) "已保存，启动 VPN 服务后生效"
            else if (stored) "名单已保存，代理重启失败，请手动重新连接"
            else "保存失败，请重试"
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AccessControlViewModel(StoredAccessControlRepository(context.applicationContext)) as T
        }
    }
}
