package com.github.kr328.clash

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.OverrideSettingsDesign
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.util.withClash
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.ConfigChoice
import com.koude.aurora.ui.settings.ConfigField
import com.koude.aurora.ui.settings.ConfigFieldEditor
import com.koude.aurora.ui.settings.OverrideFormScreen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class OverrideSettingsActivity : BaseActivity<OverrideSettingsDesign>() {
    override suspend fun main() {
        val configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }
        val revision = mutableIntStateOf(0)

        defer { withClash { patchOverride(Clash.OverrideSlot.Persist, configuration) } }

        setContent {
            val currentRevision = revision.intValue
            @Suppress("UNUSED_VARIABLE") val keepStateRead = currentRevision
            AuroraTheme {
                OverrideFormScreen(
                    title = getString(DesignR.string.override),
                    fields = overrideFields(configuration) { revision.intValue++ },
                    onBack = ::finish,
                    onReset = {
                        launch {
                            defer { withClash { clearOverride(Clash.OverrideSlot.Persist) } }
                            finish()
                        }
                    },
                )
            }
        }

        while (isActive) {
            select<Unit> { events.onReceive { if (it == Event.ServiceRecreated) finish() } }
        }
    }

    private fun overrideFields(config: ConfigurationOverride, changed: () -> Unit): List<ConfigField> {
        val noChange = getString(DesignR.string.dont_modify)
        val general = getString(DesignR.string.general)
        val dns = getString(DesignR.string.dns)
        val disabled = getString(DesignR.string.disabled)

        fun text(section: String, title: Int, current: String?, emptyLabel: String = getString(DesignR.string.default_), set: (String?) -> Unit) = ConfigField(
            section = section,
            title = getString(title),
            value = current?.let { if (it.isEmpty()) emptyLabel else it } ?: noChange,
            editor = ConfigFieldEditor.Text,
            initialText = current.orEmpty(),
            onSet = { set(it); changed() },
        )
        fun lines(section: String, title: Int, current: List<String>?, set: (List<String>?) -> Unit) = ConfigField(
            section = section,
            title = getString(title),
            value = current?.let { if (it.isEmpty()) getString(DesignR.string.empty) else getString(DesignR.string.format_elements, it.size) } ?: noChange,
            editor = ConfigFieldEditor.Lines,
            initialText = current?.joinToString("\n").orEmpty(),
            onSet = { value -> set(value?.lines()?.filter(String::isNotBlank)); changed() },
        )
        fun choice(section: String, title: Int, current: String?, options: List<ConfigChoice>, set: (String?) -> Unit) = ConfigField(
            section = section,
            title = getString(title),
            value = options.firstOrNull { it.value == current }?.label ?: noChange,
            editor = ConfigFieldEditor.Choices,
            choices = options,
            selectedValue = current,
            onSet = { set(it); changed() },
        )
        fun map(section: String, title: Int, current: Map<String, String>?, set: (Map<String, String>?) -> Unit) = ConfigField(
            section = section,
            title = getString(title),
            value = current?.let { if (it.isEmpty()) getString(DesignR.string.empty) else getString(DesignR.string.format_elements, it.size) } ?: noChange,
            editor = ConfigFieldEditor.Lines,
            initialText = current?.entries?.joinToString("\n") { "${it.key}=${it.value}" }.orEmpty(),
            onSet = { value ->
                set(value?.lines()?.mapNotNull { line -> line.substringBefore("=").trim().takeIf(String::isNotEmpty)?.let { it to line.substringAfter("=", "").trim() } }?.toMap())
                changed()
            },
        )
        fun boolOptions(falseLabel: String = getString(DesignR.string.disabled), trueLabel: String = getString(DesignR.string.enabled)) = listOf(
            ConfigChoice(null, noChange), ConfigChoice("true", trueLabel), ConfigChoice("false", falseLabel),
        )
        fun bool(section: String, title: Int, current: Boolean?, falseLabel: String = disabled, set: (Boolean?) -> Unit) = choice(
            section, title, current?.toString(), boolOptions(falseLabel), { set(it?.toBooleanStrictOrNull()) },
        )

        val fields = mutableListOf<ConfigField>()
        fun intPort(title: Int, get: Int?, set: (Int?) -> Unit) = fields.add(text(general, title, get?.let { if (it == 0) "" else it.toString() }, disabled) { value -> set(value?.let { it.toIntOrNull() ?: 0 }) })
        intPort(DesignR.string.http_port, config.httpPort) { config.httpPort = it }
        intPort(DesignR.string.socks_port, config.socksPort) { config.socksPort = it }
        intPort(DesignR.string.redirect_port, config.redirectPort) { config.redirectPort = it }
        intPort(DesignR.string.tproxy_port, config.tproxyPort) { config.tproxyPort = it }
        intPort(DesignR.string.mixed_port, config.mixedPort) { config.mixedPort = it }
        fields += lines(general, DesignR.string.authentication, config.authentication) { config.authentication = it }
        fields += bool(general, DesignR.string.allow_lan, config.allowLan) { config.allowLan = it }
        fields += bool(general, DesignR.string.ipv6, config.ipv6) { config.ipv6 = it }
        fields += text(general, DesignR.string.bind_address, config.bindAddress) { config.bindAddress = it }
        fields += text(general, DesignR.string.external_controller, config.externalController) { config.externalController = it }
        fields += text(general, DesignR.string.external_controller_tls, config.externalControllerTLS) { config.externalControllerTLS = it }
        fields += lines(general, DesignR.string.allow_origins, config.externalControllerCors.allowOrigins) { config.externalControllerCors.allowOrigins = it }
        fields += bool(general, DesignR.string.allow_private_network, config.externalControllerCors.allowPrivateNetwork) { config.externalControllerCors.allowPrivateNetwork = it }
        fields += text(general, DesignR.string.secret, config.secret) { config.secret = it }
        fields += choice(general, DesignR.string.mode, config.mode?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(TunnelState.Mode.Direct.name, getString(DesignR.string.direct_mode)),
            ConfigChoice(TunnelState.Mode.Global.name, getString(DesignR.string.global_mode)), ConfigChoice(TunnelState.Mode.Rule.name, getString(DesignR.string.rule_mode)),
        )) { config.mode = it?.let(TunnelState.Mode::valueOf) }
        fields += choice(general, DesignR.string.log_level, config.logLevel?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(LogMessage.Level.Info.name, getString(DesignR.string.info)),
            ConfigChoice(LogMessage.Level.Warning.name, getString(DesignR.string.warning)), ConfigChoice(LogMessage.Level.Error.name, getString(DesignR.string.error)),
            ConfigChoice(LogMessage.Level.Debug.name, getString(DesignR.string.debug)), ConfigChoice(LogMessage.Level.Silent.name, getString(DesignR.string.silent)),
        )) { config.logLevel = it?.let(LogMessage.Level::valueOf) }
        fields += map(general, DesignR.string.hosts, config.hosts) { config.hosts = it }

        val dnsStart = fields.size
        val dnsEnabled = config.dns.enable != false
        fields += choice(dns, DesignR.string.strategy, config.dns.enable?.toString(), listOf(
            ConfigChoice(null, noChange), ConfigChoice("true", getString(DesignR.string.force_enable)), ConfigChoice("false", getString(DesignR.string.use_built_in)),
        )) { config.dns.enable = it?.toBooleanStrictOrNull() }
        fields += bool(dns, DesignR.string.prefer_h3, config.dns.preferH3) { config.dns.preferH3 = it }
        fields += text(dns, DesignR.string.listen, config.dns.listen, disabled) { config.dns.listen = it }
        fields += bool(dns, DesignR.string.append_system_dns, config.app.appendSystemDns) { config.app.appendSystemDns = it }
        fields += bool(dns, DesignR.string.ipv6, config.dns.ipv6) { config.dns.ipv6 = it }
        fields += bool(dns, DesignR.string.use_hosts, config.dns.useHosts) { config.dns.useHosts = it }
        fields += choice(dns, DesignR.string.enhanced_mode, config.dns.enhancedMode?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(ConfigurationOverride.DnsEnhancedMode.None.name, disabled),
            ConfigChoice(ConfigurationOverride.DnsEnhancedMode.FakeIp.name, getString(DesignR.string.fakeip)), ConfigChoice(ConfigurationOverride.DnsEnhancedMode.Mapping.name, getString(DesignR.string.mapping)),
        )) { config.dns.enhancedMode = it?.let(ConfigurationOverride.DnsEnhancedMode::valueOf) }
        fields += lines(dns, DesignR.string.name_server, config.dns.nameServer) { config.dns.nameServer = it }
        fields += lines(dns, DesignR.string.fallback, config.dns.fallback) { config.dns.fallback = it }
        fields += lines(dns, DesignR.string.default_name_server, config.dns.defaultServer) { config.dns.defaultServer = it }
        fields += lines(dns, DesignR.string.fakeip_filter, config.dns.fakeIpFilter) { config.dns.fakeIpFilter = it }
        fields += choice(dns, DesignR.string.fakeip_filter_mode, config.dns.fakeIPFilterMode?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(ConfigurationOverride.FilterMode.BlackList.name, getString(DesignR.string.blacklist)),
            ConfigChoice(ConfigurationOverride.FilterMode.WhiteList.name, getString(DesignR.string.whitelist)), ConfigChoice(ConfigurationOverride.FilterMode.Rule.name, getString(DesignR.string.rule)),
        )) { config.dns.fakeIPFilterMode = it?.let(ConfigurationOverride.FilterMode::valueOf) }
        fields += bool(dns, DesignR.string.geoip_fallback, config.dns.fallbackFilter.geoIp) { config.dns.fallbackFilter.geoIp = it }
        fields += text(dns, DesignR.string.geoip_fallback_code, config.dns.fallbackFilter.geoIpCode, getString(DesignR.string.raw_cn)) { config.dns.fallbackFilter.geoIpCode = it }
        fields += lines(dns, DesignR.string.domain_fallback, config.dns.fallbackFilter.domain) { config.dns.fallbackFilter.domain = it }
        fields += lines(dns, DesignR.string.ipcidr_fallback, config.dns.fallbackFilter.ipcidr) { config.dns.fallbackFilter.ipcidr = it }
        fields += map(dns, DesignR.string.name_server_policy, config.dns.nameserverPolicy) { config.dns.nameserverPolicy = it }

        if (!dnsEnabled) {
            for (index in dnsStart + 1 until fields.size) fields[index] = fields[index].copy(enabled = false)
        }
        return fields
    }
}
