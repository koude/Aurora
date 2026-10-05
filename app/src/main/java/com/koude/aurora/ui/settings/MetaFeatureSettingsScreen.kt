package com.koude.aurora.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.design.R as DesignR

@Composable
fun MetaFeatureSettingsScreen(
    fields: List<ConfigField>,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onImportGeo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ageHelper by remember { mutableStateOf<Boolean?>(null) }
    val actions = listOf(
        ConfigAction("age_x25519", stringResource(DesignR.string.age_key_category), stringResource(DesignR.string.age_key_type_x25519), stringResource(DesignR.string.age_key_generate_summary)),
        ConfigAction("age_hybrid", stringResource(DesignR.string.age_key_category), stringResource(DesignR.string.age_key_type_hybrid), stringResource(DesignR.string.age_key_generate_summary)),
        ConfigAction("geoip", stringResource(DesignR.string.geox_files), stringResource(DesignR.string.import_geoip_file), stringResource(DesignR.string.press_to_import)),
        ConfigAction("geosite", stringResource(DesignR.string.geox_files), stringResource(DesignR.string.import_geosite_file), stringResource(DesignR.string.press_to_import)),
        ConfigAction("country", stringResource(DesignR.string.geox_files), stringResource(DesignR.string.import_country_file), stringResource(DesignR.string.press_to_import)),
        ConfigAction("asn", stringResource(DesignR.string.geox_files), stringResource(DesignR.string.import_asn_file), stringResource(DesignR.string.press_to_import)),
    )
    OverrideFormScreen(
        title = stringResource(DesignR.string.meta_features),
        fields = fields,
        actions = actions,
        onAction = { id ->
            when (id) {
                "age_x25519" -> ageHelper = false
                "age_hybrid" -> ageHelper = true
                else -> onImportGeo(id)
            }
        },
        onBack = onBack,
        onReset = onReset,
        modifier = modifier,
    )
    ageHelper?.let { hybrid -> AgeKeyHelperDialog(hybrid, onDismiss = { ageHelper = null }) }
}

@Composable
private fun AgeKeyHelperDialog(hybrid: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var secretKey by remember(hybrid) { mutableStateOf("") }
    var publicKey by remember(hybrid) { mutableStateOf("") }
    val secretValid = secretKey.isBlank() || Clash.veritySecretKeys(secretKey)
    val publicValid = publicKey.isBlank() || Clash.verityPublicKeys(publicKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (hybrid) DesignR.string.age_key_type_hybrid else DesignR.string.age_key_type_x25519)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(DesignR.string.age_secret_key)) },
                    singleLine = true,
                    isError = !secretValid,
                    supportingText = { if (!secretValid) Text(stringResource(DesignR.string.age_secret_key_error)) },
                )
                Row {
                    TextButton(onClick = {
                        val pair = if (hybrid) Clash.genHybridKeyPair() else Clash.genX25519KeyPair()
                        secretKey = pair.secretKey
                        publicKey = pair.publicKey
                    }) { Text(stringResource(DesignR.string.age_key_generate)) }
                    TextButton(onClick = { copyKey(context, "age_secret_key", secretKey) }) { Text(stringResource(DesignR.string.age_key_copy)) }
                }
                OutlinedTextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(DesignR.string.age_public_key)) },
                    singleLine = true,
                    isError = !publicValid,
                    supportingText = { if (!publicValid) Text(stringResource(DesignR.string.age_public_key_error)) },
                )
                Row {
                    TextButton(onClick = { publicKey = Clash.toPublicKeys(secretKey).firstOrNull().orEmpty() }) { Text(stringResource(DesignR.string.age_key_to_public)) }
                    TextButton(onClick = { copyKey(context, "age_public_key", publicKey) }) { Text(stringResource(DesignR.string.age_key_copy)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.close)) } },
    )
}

private fun copyKey(context: android.content.Context, label: String, value: String) {
    if (value.isBlank()) return
    context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, DesignR.string.copied, Toast.LENGTH_SHORT).show()
}
