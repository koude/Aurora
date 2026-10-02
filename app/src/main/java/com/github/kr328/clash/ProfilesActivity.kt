package com.github.kr328.clash

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.ProfilesDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.ProfileProvider
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.withProfile
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRError
import io.github.g00fy2.quickie.QRResult.QRMissingPermission
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.QRResult.QRUserCanceled
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class ProfilesActivity : BaseActivity<ProfilesDesign>() {
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override suspend fun main() {
        val design = ProfilesDesign(this)

        setContentDesign(design)

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ProfileChanged -> {
                            design.fetch()
                            design.setClashRunning(clashRunning)
                        }
                        Event.ClashStart, Event.ClashStop ->
                            design.setClashRunning(clashRunning)
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        ProfilesDesign.Request.OpenHome ->
                            navigateTopLevel(MainActivity::class)
                        ProfilesDesign.Request.OpenProxy ->
                            navigateTopLevel(ProxyActivity::class)
                        ProfilesDesign.Request.OpenSettings ->
                            navigateTopLevel(SettingsActivity::class)
                        ProfilesDesign.Request.Create ->
                            design.showCreateDialog(queryProfileProviders())
                        is ProfilesDesign.Request.CreateProfile ->
                            createProfile(it.provider)
                        is ProfilesDesign.Request.CreateUrl ->
                            importProfile(Profile.Type.Url, it.name, it.url)
                        is ProfilesDesign.Request.OpenProviderDetail ->
                            launchAppDetailed(it.provider)
                        ProfilesDesign.Request.UpdateAll ->
                            withProfile {
                                try {
                                    queryAll().forEach { p ->
                                        if (p.imported && p.type != Profile.Type.File)
                                            update(p.uuid)
                                    }
                                }
                                finally {
                                    withContext(Dispatchers.Main) {
                                        design.finishUpdateAll();
                                    }
                                }
                            }
                        is ProfilesDesign.Request.Update ->
                            withProfile { update(it.profile.uuid) }
                        is ProfilesDesign.Request.Delete ->
                            withProfile { delete(it.profile.uuid) }
                        is ProfilesDesign.Request.Edit ->
                            startActivity(PropertiesActivity::class.intent.setUUID(it.profile.uuid))
                        is ProfilesDesign.Request.Active -> {
                            withProfile {
                                if (it.profile.imported)
                                    setActive(it.profile)
                                else
                                    design.requestSave(it.profile)
                            }
                        }
                        is ProfilesDesign.Request.Duplicate -> {
                            val uuid = withProfile { clone(it.profile.uuid) }

                            startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                        }
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        design.updateElapsed()
                    }
                }
            }
        }
    }

    private suspend fun ProfilesDesign.fetch() {
        withProfile {
            patchProfiles(queryAll())
        }
    }

    private suspend fun createProfile(provider: ProfileProvider) {
        when (provider) {
            is ProfileProvider.File -> importProfileFromFile()
            is ProfileProvider.Url -> design?.showCreateUrlDialog()
            is ProfileProvider.QR -> scanLauncher.launch(null)
            is ProfileProvider.External -> provider.get()?.let { (uri, initialName) ->
                importProfile(
                    Profile.Type.External,
                    initialName ?: getString(R.string.new_profile),
                    uri.toString(),
                )
            }
        }
    }

    private suspend fun importProfileFromFile() {
        val uri: Uri = startActivityForResult(
            ActivityResultContracts.GetContent(),
            "*/*",
        ) ?: return
        val fallback = getString(R.string.new_profile)
        val name = uri.fileName
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: fallback

        importProfile(Profile.Type.File, name) { uuid ->
            FilesClient(this).copyDocument("$uuid/config.yaml", uri)
        }
    }

    private suspend fun importProfile(
        type: Profile.Type,
        name: String,
        source: String = "",
        prepare: suspend (UUID) -> Unit = {},
    ) {
        val currentDesign = design ?: return
        var uuid: UUID? = null

        try {
            currentDesign.withProcessing { updateStatus ->
                uuid = withProfile { create(type, name, source) }
                prepare(uuid!!)
                withProfile {
                    coroutineScope {
                        commit(uuid!!) { status ->
                            launch { updateStatus(status) }
                        }
                    }
                }
            }
            currentDesign.fetch()
        } catch (e: Exception) {
            uuid?.let { failedUuid ->
                runCatching { withProfile { delete(failedUuid) } }
            }
            currentDesign.showExceptionToast(e)
            currentDesign.fetch()
        }
    }

    private fun launchAppDetailed(provider: ProfileProvider.External) {
        val data = Uri.fromParts(
            "package",
            provider.intent.component?.packageName ?: return,
            null,
        )

        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(data))
    }

    private suspend fun ProfileProvider.External.get(): Pair<Uri, String?>? {
        val result = startActivityForResult(
            ActivityResultContracts.StartActivityForResult(),
            intent,
        )

        if (result.resultCode != Activity.RESULT_OK) return null

        val uri = result.data?.data ?: return null
        val name = result.data?.getStringExtra(Intents.EXTRA_NAME)
        return uri to name
    }

    private suspend fun queryProfileProviders(): List<ProfileProvider> {
        return withContext(Dispatchers.IO) {
            val providers = packageManager.queryIntentActivities(
                Intent(Intents.ACTION_PROVIDE_URL),
                0,
            ).map {
                val activity = it.activityInfo
                val name = activity.applicationInfo.loadLabel(packageManager)
                val summary = activity.loadLabel(packageManager)
                val icon = activity.loadIcon(packageManager)
                val intent = Intent(Intents.ACTION_PROVIDE_URL).setComponent(
                    ComponentName(activity.packageName, activity.name),
                )

                ProfileProvider.External(name.toString(), summary.toString(), icon, intent)
            }

            listOf(
                ProfileProvider.File(this@ProfilesActivity),
                ProfileProvider.Url(this@ProfilesActivity),
                ProfileProvider.QR(this@ProfilesActivity),
            ) + providers
        }
    }

    private fun scanResultHandler(result: QRResult) {
        launch {
            when (result) {
                is QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.let { String(it) }.orEmpty()
                    createProfileByQrCode(url)
                }
                QRUserCanceled -> Unit
                QRMissingPermission -> design?.showExceptionToast(
                    getString(R.string.import_from_qr_no_permission),
                )
                is QRError -> design?.showExceptionToast(
                    getString(R.string.import_from_qr_exception),
                )
            }
        }
    }

    private suspend fun createProfileByQrCode(url: String) {
        importProfile(
            Profile.Type.Url,
            getString(R.string.new_profile),
            url,
        )
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if(uuid == null)
            return;
        launch {
            var name: String? = null;
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            design?.showToast(
                getString(R.string.toast_profile_updated_complete, name),
                ToastDuration.Long
            )
        }
    }
    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if(uuid == null)
            return;
        launch {
            var name: String? = null;
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            design?.showToast(
                getString(R.string.toast_profile_updated_failed, name, reason),
                ToastDuration.Long
            ){
                setAction(R.string.edit) {
                    startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                }
            }
        }
    }
}
