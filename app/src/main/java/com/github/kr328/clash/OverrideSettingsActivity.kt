package com.github.kr328.clash

import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.ConfigChoice
import com.koude.aurora.ui.settings.ConfigField
import com.koude.aurora.ui.settings.ConfigFieldEditor
import com.koude.aurora.ui.settings.OverrideFormScreen
import com.koude.aurora.ui.settings.OverrideEditorViewModel
import com.koude.aurora.ui.settings.OverrideMapInput
import com.koude.aurora.ui.settings.parseOverrideMapInput
import com.koude.aurora.ui.settings.parsePortOverrideInput
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class OverrideSettingsActivity : BaseActivity() {
    private val editor: OverrideEditorViewModel by viewModels { OverrideEditorViewModel.Factory }

    override suspend fun main() {
        val configuration = editor.load()

        defer { editor.save() }

        setContent {
            val currentRevision by editor.revision.collectAsState()
            @Suppress("UNUSED_VARIABLE") val keepStateRead = currentRevision
            AuroraTheme(darkTheme = isDarkTheme) {
                OverrideFormScreen(
                    title = getString(DesignR.string.override),
                    fields = overrideFields(configuration, editor::changed),
                    onBack = ::finish,
                    onReset = {
                        launch {
                            defer { editor.reset() }
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
        val ports = getString(DesignR.string.aurora_override_section_ports)
        val access = getString(DesignR.string.aurora_override_section_access)
        val controller = getString(DesignR.string.aurora_override_section_controller)
        val runtime = getString(DesignR.string.aurora_override_section_runtime)
        val dnsBasic = getString(DesignR.string.aurora_override_section_dns_basic)
        val dnsServers = getString(DesignR.string.aurora_override_section_dns_servers)
        val dnsFilter = getString(DesignR.string.aurora_override_section_dns_filter)
        val disabled = getString(DesignR.string.disabled)

        fun text(section: String, title: Int, current: String?, emptyLabel: String = getString(DesignR.string.default_), sensitive: Boolean = false, set: (String?) -> Unit) = ConfigField(
            section = section,
            title = getString(title),
            value = current?.let { if (it.isEmpty()) emptyLabel else if (sensitive) getString(DesignR.string.aurora_override_value_set) else it } ?: noChange,
            editor = ConfigFieldEditor.Text,
            initialText = current.orEmpty(),
            sensitive = sensitive,
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
            supportingText = getString(DesignR.string.aurora_override_map_hint),
            inputError = { input ->
                (parseOverrideMapInput(input) as? OverrideMapInput.Invalid)?.let {
                    getString(DesignR.string.aurora_override_map_invalid, it.lineNumber)
                }
            },
            onSet = { value ->
                if (value == null) {
                    set(null)
                    changed()
                } else {
                    (parseOverrideMapInput(value) as? OverrideMapInput.Valid)?.let {
                        set(it.entries)
                        changed()
                    }
                }
            },
        )
        fun boolOptions(falseLabel: String = getString(DesignR.string.disabled), trueLabel: String = getString(DesignR.string.enabled)) = listOf(
            ConfigChoice(null, noChange), ConfigChoice("true", trueLabel), ConfigChoice("false", falseLabel),
        )
        fun bool(section: String, title: Int, current: Boolean?, falseLabel: String = disabled, set: (Boolean?) -> Unit) = choice(
            section, title, current?.toString(), boolOptions(falseLabel), { set(it?.toBooleanStrictOrNull()) },
        )

        val fields = mutableListOf<ConfigField>()
        fun intPort(title: Int, get: Int?, set: (Int?) -> Unit) {
            fields += ConfigField(
                section = ports,
                title = getString(title),
                value = get?.let { if (it == 0) disabled else it.toString() } ?: noChange,
                editor = ConfigFieldEditor.Text,
                initialText = get?.takeIf { it != 0 }?.toString().orEmpty(),
                supportingText = getString(DesignR.string.aurora_override_port_hint),
                inputError = { input ->
                    if (parsePortOverrideInput(input) == null)
                        getString(DesignR.string.aurora_override_port_invalid) else null
                },
                onSet = { input ->
                    if (input == null) {
                        set(null)
                        changed()
                    } else {
                        parsePortOverrideInput(input)?.let {
                            set(it)
                            changed()
                        }
                    }
                },
            )
        }
        intPort(DesignR.string.http_port, config.httpPort) { config.httpPort = it }
        intPort(DesignR.string.socks_port, config.socksPort) { config.socksPort = it }
        intPort(DesignR.string.redirect_port, config.redirectPort) { config.redirectPort = it }
        intPort(DesignR.string.tproxy_port, config.tproxyPort) { config.tproxyPort = it }
        intPort(DesignR.string.mixed_port, config.mixedPort) { config.mixedPort = it }
        fields += lines(access, DesignR.string.authentication, config.authentication) { config.authentication = it }
        fields += bool(access, DesignR.string.allow_lan, config.allowLan) { config.allowLan = it }
        fields += text(access, DesignR.string.bind_address, config.bindAddress) { config.bindAddress = it }
        fields += text(controller, DesignR.string.external_controller, config.externalController) { config.externalController = it }
        fields += text(controller, DesignR.string.external_controller_tls, config.externalControllerTLS) { config.externalControllerTLS = it }
        fields += lines(controller, DesignR.string.allow_origins, config.externalControllerCors.allowOrigins) { config.externalControllerCors.allowOrigins = it }
        fields += bool(controller, DesignR.string.allow_private_network, config.externalControllerCors.allowPrivateNetwork) { config.externalControllerCors.allowPrivateNetwork = it }
        fields += text(controller, DesignR.string.secret, config.secret, sensitive = true) { config.secret = it }
        fields += bool(runtime, DesignR.string.ipv6, config.ipv6) { config.ipv6 = it }
        fields += choice(runtime, DesignR.string.mode, config.mode?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(TunnelState.Mode.Direct.name, getString(DesignR.string.direct_mode)),
            ConfigChoice(TunnelState.Mode.Global.name, getString(DesignR.string.global_mode)), ConfigChoice(TunnelState.Mode.Rule.name, getString(DesignR.string.rule_mode)),
        )) { config.mode = it?.let(TunnelState.Mode::valueOf) }
        fields += choice(runtime, DesignR.string.log_level, config.logLevel?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(LogMessage.Level.Info.name, getString(DesignR.string.info)),
            ConfigChoice(LogMessage.Level.Warning.name, getString(DesignR.string.warning)), ConfigChoice(LogMessage.Level.Error.name, getString(DesignR.string.error)),
            ConfigChoice(LogMessage.Level.Debug.name, getString(DesignR.string.debug)), ConfigChoice(LogMessage.Level.Silent.name, getString(DesignR.string.silent)),
        )) { config.logLevel = it?.let(LogMessage.Level::valueOf) }
        fields += map(runtime, DesignR.string.hosts, config.hosts) { config.hosts = it }

        val dnsStart = fields.size
        val dnsEnabled = config.dns.enable != false
        fields += choice(dnsBasic, DesignR.string.strategy, config.dns.enable?.toString(), listOf(
            ConfigChoice(null, noChange), ConfigChoice("true", getString(DesignR.string.force_enable)), ConfigChoice("false", getString(DesignR.string.use_built_in)),
        )) { config.dns.enable = it?.toBooleanStrictOrNull() }
        fields += bool(dnsBasic, DesignR.string.prefer_h3, config.dns.preferH3) { config.dns.preferH3 = it }
        fields += text(dnsBasic, DesignR.string.listen, config.dns.listen, disabled) { config.dns.listen = it }
        fields += bool(dnsBasic, DesignR.string.append_system_dns, config.app.appendSystemDns) { config.app.appendSystemDns = it }
        fields += bool(dnsBasic, DesignR.string.ipv6, config.dns.ipv6) { config.dns.ipv6 = it }
        fields += bool(dnsBasic, DesignR.string.use_hosts, config.dns.useHosts) { config.dns.useHosts = it }
        fields += choice(dnsBasic, DesignR.string.enhanced_mode, config.dns.enhancedMode?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(ConfigurationOverride.DnsEnhancedMode.None.name, disabled),
            ConfigChoice(ConfigurationOverride.DnsEnhancedMode.FakeIp.name, getString(DesignR.string.fakeip)), ConfigChoice(ConfigurationOverride.DnsEnhancedMode.Mapping.name, getString(DesignR.string.mapping)),
        )) { config.dns.enhancedMode = it?.let(ConfigurationOverride.DnsEnhancedMode::valueOf) }
        fields += lines(dnsServers, DesignR.string.name_server, config.dns.nameServer) { config.dns.nameServer = it }
        fields += lines(dnsServers, DesignR.string.fallback, config.dns.fallback) { config.dns.fallback = it }
        fields += lines(dnsServers, DesignR.string.default_name_server, config.dns.defaultServer) { config.dns.defaultServer = it }
        fields += lines(dnsFilter, DesignR.string.fakeip_filter, config.dns.fakeIpFilter) { config.dns.fakeIpFilter = it }
        fields += choice(dnsFilter, DesignR.string.fakeip_filter_mode, config.dns.fakeIPFilterMode?.name, listOf(
            ConfigChoice(null, noChange), ConfigChoice(ConfigurationOverride.FilterMode.BlackList.name, getString(DesignR.string.blacklist)),
            ConfigChoice(ConfigurationOverride.FilterMode.WhiteList.name, getString(DesignR.string.whitelist)), ConfigChoice(ConfigurationOverride.FilterMode.Rule.name, getString(DesignR.string.rule)),
        )) { config.dns.fakeIPFilterMode = it?.let(ConfigurationOverride.FilterMode::valueOf) }
        fields += bool(dnsFilter, DesignR.string.geoip_fallback, config.dns.fallbackFilter.geoIp) { config.dns.fallbackFilter.geoIp = it }
        fields += text(dnsFilter, DesignR.string.geoip_fallback_code, config.dns.fallbackFilter.geoIpCode, getString(DesignR.string.raw_cn)) { config.dns.fallbackFilter.geoIpCode = it }
        fields += lines(dnsFilter, DesignR.string.domain_fallback, config.dns.fallbackFilter.domain) { config.dns.fallbackFilter.domain = it }
        fields += lines(dnsFilter, DesignR.string.ipcidr_fallback, config.dns.fallbackFilter.ipcidr) { config.dns.fallbackFilter.ipcidr = it }
        fields += map(dnsServers, DesignR.string.name_server_policy, config.dns.nameserverPolicy) { config.dns.nameserverPolicy = it }

        if (!dnsEnabled) {
            for (index in dnsStart + 1 until fields.size) fields[index] = fields[index].copy(enabled = false)
        }
        return fields
    }
}
