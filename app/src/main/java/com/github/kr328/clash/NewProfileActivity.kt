package com.github.kr328.clash

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.ProfileProvider
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.profiles.NewProfileScreen
import com.koude.aurora.ui.profiles.NewProfileViewModel
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRError
import io.github.g00fy2.quickie.QRResult.QRMissingPermission
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.QRResult.QRUserCanceled
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.UUID

class NewProfileActivity : BaseActivity() {
    private val creator: NewProfileViewModel by viewModels { NewProfileViewModel.Factory }
    private val providersState = mutableStateOf<List<ProfileProvider>>(emptyList())
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override suspend fun main() {
        providersState.value = queryProfileProviders()
        setContent {
            AuroraTheme {
                NewProfileScreen(
                    providers = providersState.value,
                    onBack = ::finish,
                    onSelect = { provider -> launch { createProfile(provider) } },
                    onProviderDetails = ::launchAppDetailed,
                )
            }
        }
        while (isActive) select<Unit> { events.onReceive { } }
    }

    private suspend fun createProfile(provider: ProfileProvider) {
        try {
            val name = getString(R.string.new_profile)
            val uuid: UUID? = when (provider) {
                is ProfileProvider.File -> creator.createFile(name)
                is ProfileProvider.Url -> creator.createUrl(name)
                is ProfileProvider.QR -> null.also { scanLauncher.launch(null) }
                is ProfileProvider.External -> provider.get()?.let { (uri, initialName) ->
                    creator.createExternal(initialName ?: name, uri.toString())
                }
            }
            if (uuid != null) launchProperties(uuid)
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "无法添加配置", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchAppDetailed(provider: ProfileProvider.External) {
        val data = Uri.fromParts("package", provider.intent.component?.packageName ?: return, null)
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(data))
    }

    private suspend fun launchProperties(uuid: UUID) {
        val result = startActivityForResult(ActivityResultContracts.StartActivityForResult(), PropertiesActivity::class.intent.setUUID(uuid))
        if (result.resultCode == Activity.RESULT_OK) finish()
    }

    private suspend fun ProfileProvider.External.get(): Pair<Uri, String?>? {
        val result = startActivityForResult(ActivityResultContracts.StartActivityForResult(), intent)
        if (result.resultCode != RESULT_OK) return null
        val uri = result.data?.data ?: return null
        return uri to result.data?.getStringExtra(Intents.EXTRA_NAME)
    }

    private suspend fun queryProfileProviders(): List<ProfileProvider> = withContext(Dispatchers.IO) {
        val providers = packageManager.queryIntentActivities(Intent(Intents.ACTION_PROVIDE_URL), 0).map {
            val activity = it.activityInfo
            val name = activity.applicationInfo.loadLabel(packageManager)
            val summary = activity.loadLabel(packageManager)
            val icon = activity.loadIcon(packageManager)
            val providerIntent = Intent(Intents.ACTION_PROVIDE_URL).setComponent(ComponentName(activity.packageName, activity.name))
            ProfileProvider.External(name.toString(), summary.toString(), icon, providerIntent)
        }
        listOf(ProfileProvider.File(this@NewProfileActivity), ProfileProvider.Url(this@NewProfileActivity), ProfileProvider.QR(this@NewProfileActivity)) + providers
    }

    private fun scanResultHandler(result: QRResult) {
        lifecycleScope.launch {
            when (result) {
                is QRSuccess -> {
                    val url = result.content.rawValue ?: result.content.rawBytes?.let { String(it) }.orEmpty()
                    createProfileByQrCode(url)
                }
                QRUserCanceled -> Unit
                QRMissingPermission -> Toast.makeText(this@NewProfileActivity, R.string.import_from_qr_no_permission, Toast.LENGTH_LONG).show()
                is QRError -> Toast.makeText(this@NewProfileActivity, R.string.import_from_qr_exception, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun createProfileByQrCode(url: String) {
        launchProperties(creator.createUrl(getString(R.string.new_profile), url))
    }
}
