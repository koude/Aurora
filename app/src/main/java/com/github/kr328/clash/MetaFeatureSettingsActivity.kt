package com.github.kr328.clash

import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.data.settings.GeoImportResult
import com.koude.aurora.data.settings.validGeoExtensions
import com.koude.aurora.ui.settings.ConfigChoice
import com.koude.aurora.ui.settings.ConfigField
import com.koude.aurora.ui.settings.ConfigFieldEditor
import com.koude.aurora.ui.settings.MetaFeatureSettingsScreen
import com.koude.aurora.ui.settings.OverrideEditorViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class MetaFeatureSettingsActivity : BaseActivity() {
    private val editor: OverrideEditorViewModel by viewModels { OverrideEditorViewModel.factoryWithGeo(this) }

    override suspend fun main() {
        val configuration = editor.load()
        defer { editor.save() }

        setContent {
            val revisionSnapshot by editor.revision.collectAsState()
            @Suppress("UNUSED_VARIABLE") val keepStateRead = revisionSnapshot
            AuroraTheme(darkTheme = isDarkTheme) {
                MetaFeatureSettingsScreen(
                    fields = metaFields(configuration, editor::changed),
                    onBack = ::finish,
                    onReset = {
                        launch {
                            defer { editor.reset() }
                            finish()
                        }
                    },
                    onImportGeo = { kind -> launch { chooseGeoFile(kind) } },
                )
            }
        }

        while (isActive) {
            select<Unit> { events.onReceive { if (it == Event.ServiceRecreated) finish() } }
        }
    }

    private fun metaFields(config: ConfigurationOverride, changed: () -> Unit): List<ConfigField> {
        val noChange = getString(DesignR.string.dont_modify)
        val settings = getString(DesignR.string.settings)
        val sniffer = getString(DesignR.string.sniffer_setting)
        val fields = mutableListOf<ConfigField>()

        fun bool(section: String, title: Int, current: Boolean?, disabledLabel: String = getString(DesignR.string.disabled), enabled: Boolean = true, set: (Boolean?) -> Unit) {
            val choices = listOf(
                ConfigChoice(null, noChange),
                ConfigChoice("true", getString(DesignR.string.enabled)),
                ConfigChoice("false", disabledLabel),
            )
            val currentString = current?.toString()
            fields += ConfigField(
                section = section,
                title = getString(title),
                value = choices.first { it.value == currentString }.label,
                editor = ConfigFieldEditor.Choices,
                choices = choices,
                selectedValue = currentString,
                enabled = enabled,
                onSet = { set(it?.toBooleanStrictOrNull()); changed() },
            )
        }
        fun mode(title: Int, current: ConfigurationOverride.FindProcessMode?, set: (ConfigurationOverride.FindProcessMode?) -> Unit) {
            val choices = listOf(
                ConfigChoice(null, noChange),
                ConfigChoice(ConfigurationOverride.FindProcessMode.Off.name, getString(DesignR.string.off)),
                ConfigChoice(ConfigurationOverride.FindProcessMode.Strict.name, getString(DesignR.string.strict)),
                ConfigChoice(ConfigurationOverride.FindProcessMode.Always.name, getString(DesignR.string.always)),
            )
            fields += ConfigField(
                section = settings,
                title = getString(title),
                value = choices.first { it.value == current?.name }.label,
                editor = ConfigFieldEditor.Choices,
                choices = choices,
                selectedValue = current?.name,
                onSet = { set(it?.let(ConfigurationOverride.FindProcessMode::valueOf)); changed() },
            )
        }
        fun lines(section: String, title: Int, current: List<String>?, set: (List<String>?) -> Unit, enabled: Boolean = true) {
            fields += ConfigField(
                section = section,
                title = getString(title),
                value = current?.let { if (it.isEmpty()) getString(DesignR.string.empty) else getString(DesignR.string.format_elements, it.size) } ?: noChange,
                editor = ConfigFieldEditor.Lines,
                initialText = current?.joinToString("\n").orEmpty(),
                enabled = enabled,
                onSet = { text -> set(text?.lines()?.filter(String::isNotBlank)); changed() },
            )
        }

        bool(settings, DesignR.string.unified_delay, config.unifiedDelay) { config.unifiedDelay = it }
        bool(settings, DesignR.string.geodata_mode, config.geodataMode) { config.geodataMode = it }
        bool(settings, DesignR.string.tcp_concurrent, config.tcpConcurrent) { config.tcpConcurrent = it }
        mode(DesignR.string.find_process_mode, config.findProcessMode) { config.findProcessMode = it }

        val snifferEnabled = config.sniffer.enable != false
        bool(sniffer, DesignR.string.strategy, config.sniffer.enable) { config.sniffer.enable = it }
        lines(sniffer, DesignR.string.sniff_http_ports, config.sniffer.sniff.http.ports, { config.sniffer.sniff.http.ports = it }, snifferEnabled)
        bool(sniffer, DesignR.string.sniff_http_override_destination, config.sniffer.sniff.http.overrideDestination, enabled = snifferEnabled) { config.sniffer.sniff.http.overrideDestination = it }
        lines(sniffer, DesignR.string.sniff_tls_ports, config.sniffer.sniff.tls.ports, { config.sniffer.sniff.tls.ports = it }, snifferEnabled)
        bool(sniffer, DesignR.string.sniff_tls_override_destination, config.sniffer.sniff.tls.overrideDestination, enabled = snifferEnabled) { config.sniffer.sniff.tls.overrideDestination = it }
        lines(sniffer, DesignR.string.sniff_quic_ports, config.sniffer.sniff.quic.ports, { config.sniffer.sniff.quic.ports = it }, snifferEnabled)
        bool(sniffer, DesignR.string.sniff_quic_override_destination, config.sniffer.sniff.quic.overrideDestination, enabled = snifferEnabled) { config.sniffer.sniff.quic.overrideDestination = it }
        bool(sniffer, DesignR.string.force_dns_mapping, config.sniffer.forceDnsMapping, enabled = snifferEnabled) { config.sniffer.forceDnsMapping = it }
        bool(sniffer, DesignR.string.parse_pure_ip, config.sniffer.parsePureIp, enabled = snifferEnabled) { config.sniffer.parsePureIp = it }
        bool(sniffer, DesignR.string.override_destination, config.sniffer.overrideDestination, enabled = snifferEnabled) { config.sniffer.overrideDestination = it }
        lines(sniffer, DesignR.string.force_domain, config.sniffer.forceDomain, { config.sniffer.forceDomain = it }, snifferEnabled)
        lines(sniffer, DesignR.string.skip_domain, config.sniffer.skipDomain, { config.sniffer.skipDomain = it }, snifferEnabled)
        lines(sniffer, DesignR.string.skip_src_address, config.sniffer.skipSrcAddress, { config.sniffer.skipSrcAddress = it }, snifferEnabled)
        lines(sniffer, DesignR.string.skip_dst_address, config.sniffer.skipDstAddress, { config.sniffer.skipDstAddress = it }, snifferEnabled)
        return fields
    }

    private suspend fun chooseGeoFile(kind: String) {
        if (kind !in listOf("geoip", "geosite", "country", "asn")) return
        val uri = startActivityForResult(ActivityResultContracts.GetContent(), "*/*") ?: return
        val message = when (val result = editor.importGeo(uri, kind)) {
            is GeoImportResult.Imported -> getString(DesignR.string.geofile_imported, result.displayName)
            GeoImportResult.UnsupportedFormat -> getString(
                DesignR.string.geofile_unknown_db_format_message, validGeoExtensions.joinToString("/"),
            )
            GeoImportResult.Failed -> getString(DesignR.string.geofile_import_failed)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
