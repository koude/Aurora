package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.model.RoutePreview
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.databinding.DialogRouteTestBinding
import com.github.kr328.clash.design.dialog.AppBottomSheetDialog
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.MainNavigationDestination
import com.github.kr328.clash.design.util.configureMainNavigation
import com.github.kr328.clash.design.util.setProxyNavigationEnabled
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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
        TestSiteLatency,
        OpenRouteTest,
        Placeholder,
    }

    enum class LatencySite {
        Apple,
        GitHub,
        YouTube,
        Google,
    }

    private var currentMode = TunnelState.Mode.Rule
    private var clashIsRunning = false
    private var routeTestBinding: DialogRouteTestBinding? = null

    val routePreviewRequests = Channel<String>(Channel.BUFFERED)

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
            clashIsRunning = running
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

    suspend fun setLatencyTesting(testing: Boolean) {
        withContext(Dispatchers.Main) {
            binding.latencyTesting = testing

            if (testing) {
                val pending = context.getString(R.string.aurora_latency_testing)
                binding.appleLatency = pending
                binding.githubLatency = pending
                binding.youtubeLatency = pending
                binding.googleLatency = pending
            }
        }
    }

    suspend fun setSiteLatency(site: LatencySite, latencyMillis: Long?) {
        withContext(Dispatchers.Main) {
            val value = latencyMillis?.let {
                context.getString(R.string.aurora_latency_value, it)
            } ?: context.getString(R.string.aurora_latency_timeout)

            when (site) {
                LatencySite.Apple -> binding.appleLatency = value
                LatencySite.GitHub -> binding.githubLatency = value
                LatencySite.YouTube -> binding.youtubeLatency = value
                LatencySite.Google -> binding.googleLatency = value
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

    fun showRouteTest() {
        val dialog = AppBottomSheetDialog(context)
        val routeBinding = DialogRouteTestBinding.inflate(context.layoutInflater)
        routeTestBinding = routeBinding

        routeBinding.routeTestButton.setOnClickListener {
            val target = routeBinding.routeTarget.text?.toString().orEmpty().trim()
            val host = normalizeRouteTarget(target)

            if (host == null) {
                routeBinding.routeTargetLayout.error = context.getString(R.string.aurora_route_invalid_target)
                return@setOnClickListener
            }

            if (!clashIsRunning) {
                routeBinding.routeTargetLayout.error = context.getString(R.string.aurora_route_service_required)
                return@setOnClickListener
            }

            routeBinding.routeTargetLayout.error = null
            routeBinding.routeTargetRow.value = host
            routeBinding.routeRuleRow.value = context.getString(R.string.aurora_route_testing)
            routeBinding.routePolicyRow.value = context.getString(R.string.aurora_route_result_unknown)
            routeBinding.routeOutboundValue.text = context.getString(R.string.aurora_route_result_unknown)
            routeBinding.routeEmpty.visibility = View.GONE
            routeBinding.routeResult.visibility = View.VISIBLE
            routeBinding.routeTestButton.isEnabled = false
            routeBinding.routeTestButton.setText(R.string.aurora_testing)
            routePreviewRequests.trySend(host)
        }

        dialog.setContentView(routeBinding.root)
        dialog.setOnDismissListener {
            if (routeTestBinding === routeBinding) routeTestBinding = null
        }
        dialog.show()
    }

    suspend fun setRoutePreview(preview: RoutePreview) {
        withContext(Dispatchers.Main) {
            routeTestBinding?.apply {
                routeTestButton.isEnabled = true
                routeTestButton.setText(R.string.aurora_route_test_action)

                if (preview.error != null) {
                    routeTargetLayout.error = preview.error
                    return@apply
                }

                routeTargetLayout.error = null
                routeTargetRow.value = preview.resolvedIp?.let { "${preview.target} · $it" } ?: preview.target
                routeRuleRow.value = preview.rule
                routePolicyRow.value = preview.policy
                routeOutboundValue.text = preview.outbound
            }
        }
    }

    suspend fun setRoutePreviewError() {
        withContext(Dispatchers.Main) {
            routeTestBinding?.apply {
                routeTestButton.isEnabled = true
                routeTestButton.setText(R.string.aurora_route_test_action)
                routeTargetLayout.error = context.getString(R.string.aurora_route_test_failed)
            }
        }
    }

    private fun normalizeRouteTarget(value: String): String? {
        if (value.isBlank() || value.any(Char::isWhitespace)) return null

        return runCatching {
            val uri = java.net.URI(if ("://" in value) value else "https://$value")
            uri.host?.trimEnd('.')?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    init {
        binding.self = this
        binding.downloadSpeed = context.getString(R.string.aurora_speed_placeholder)
        binding.uploadSpeed = context.getString(R.string.aurora_speed_placeholder)
        binding.mode = context.getString(R.string.aurora_mode_rule)
        binding.latencyTesting = false
        binding.appleLatency = context.getString(R.string.aurora_latency_placeholder)
        binding.githubLatency = context.getString(R.string.aurora_latency_placeholder)
        binding.youtubeLatency = context.getString(R.string.aurora_latency_placeholder)
        binding.googleLatency = context.getString(R.string.aurora_latency_placeholder)
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
