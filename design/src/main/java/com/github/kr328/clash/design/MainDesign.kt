package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.MainNavigationDestination
import com.github.kr328.clash.design.util.configureMainNavigation
import com.github.kr328.clash.design.util.setProxyNavigationEnabled
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus,
        OpenProxy,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
        SetModeRule,
        SetModeGlobal,
        SetModeDirect,
        Placeholder,
    }

    private var currentMode = TunnelState.Mode.Rule

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running
            binding.modeButton.isEnabled = running
            binding.connectionButton.backgroundTintList = ColorStateList.valueOf(
                context.resolveThemedColor(
                    if (running) com.google.android.material.R.attr.colorPrimary
                    else R.attr.colorClashStopped
                )
            )
            binding.navigation.setProxyNavigationEnabled(running)
        }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) {
            binding.forwarded = value.trafficTotal()
        }
    }

    suspend fun setMode(mode: TunnelState.Mode) {
        withContext(Dispatchers.Main) {
            currentMode = mode
            binding.mode = when (mode) {
                TunnelState.Mode.Direct -> context.getString(R.string.aurora_mode_direct)
                TunnelState.Mode.Global -> context.getString(R.string.aurora_mode_global)
                TunnelState.Mode.Rule -> context.getString(R.string.aurora_mode_rule)
                else -> context.getString(R.string.aurora_mode_rule)
            }
        }
    }

    private fun showModeMenu() {
        PopupMenu(context, binding.modeButton).apply {
            menuInflater.inflate(R.menu.menu_main_mode, menu)
            when (currentMode) {
                TunnelState.Mode.Direct -> menu.findItem(R.id.mode_direct).isChecked = true
                TunnelState.Mode.Global -> menu.findItem(R.id.mode_global).isChecked = true
                TunnelState.Mode.Rule -> menu.findItem(R.id.mode_rule).isChecked = true
                else -> menu.findItem(R.id.mode_rule).isChecked = true
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.mode_direct -> request(Request.SetModeDirect)
                    R.id.mode_global -> request(Request.SetModeGlobal)
                    R.id.mode_rule -> request(Request.SetModeRule)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    suspend fun setHasProviders(has: Boolean) {
        withContext(Dispatchers.Main) {
            binding.hasProviders = has
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(binding.root)
                .show()
        }
    }

    init {
        binding.self = this
        binding.downloadSpeed = context.getString(R.string.aurora_speed_placeholder)
        binding.uploadSpeed = context.getString(R.string.aurora_speed_placeholder)
        binding.mode = context.getString(R.string.aurora_mode_rule)
        binding.modeButton.setOnClickListener { showModeMenu() }

        binding.navigation.configureMainNavigation(MainNavigationDestination.Home) {
            when (it) {
                MainNavigationDestination.Home -> Unit
                MainNavigationDestination.Proxy -> request(Request.OpenProxy)
                MainNavigationDestination.Profiles -> request(Request.OpenProfiles)
                MainNavigationDestination.Settings -> request(Request.OpenSettings)
            }
        }
    }

    fun request(request: Request) {
        requests.trySend(request)
    }
}
