package com.github.kr328.clash.design

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.github.kr328.clash.design.adapter.ProfileAdapter
import com.github.kr328.clash.design.adapter.ProfileProviderAdapter
import com.github.kr328.clash.design.databinding.DesignProfilesBinding
import com.github.kr328.clash.design.databinding.DialogCreateUrlProfileBinding
import com.github.kr328.clash.design.databinding.DialogProfileProvidersBinding
import com.github.kr328.clash.design.databinding.DialogProfilesMenuBinding
import com.github.kr328.clash.design.dialog.AppBottomSheetDialog
import com.github.kr328.clash.design.dialog.ModelProgressBarConfigure
import com.github.kr328.clash.design.dialog.withModelProgressBar
import com.github.kr328.clash.design.model.ProfileProvider
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.*
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfilesDesign(context: Context) : Design<ProfilesDesign.Request>(context) {
    sealed class Request {
        object UpdateAll : Request()
        object Create : Request()
        object OpenHome : Request()
        object OpenProxy : Request()
        object OpenSettings : Request()
        data class CreateProfile(val provider: ProfileProvider) : Request()
        data class CreateUrl(val name: String, val url: String) : Request()
        data class OpenProviderDetail(val provider: ProfileProvider.External) : Request()
        data class Active(val profile: Profile) : Request()
        data class Update(val profile: Profile) : Request()
        data class Edit(val profile: Profile) : Request()
        data class Duplicate(val profile: Profile) : Request()
        data class Delete(val profile: Profile) : Request()
    }

    private val binding = DesignProfilesBinding
        .inflate(context.layoutInflater, context.root, false)
    private val adapter = ProfileAdapter(context, this::requestActive, this::showMenu)

    private var allUpdating: Boolean
        get() = adapter.states.allUpdating;
        set(value) {
            adapter.states.allUpdating = value
        }
    private val rotateAnimation : Animation = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite)

    override val root: View
        get() = binding.root

    suspend fun patchProfiles(profiles: List<Profile>) {
        adapter.apply {
            patchDataSet(this::profiles, profiles, id = { it.uuid })
        }

        val updatable = withContext(Dispatchers.Default) {
            profiles.any { it.imported && it.type != Profile.Type.File }
        }

        withContext(Dispatchers.Main) {
            binding.updateView.visibility = if (updatable) View.VISIBLE else View.GONE
            binding.emptyView.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    suspend fun requestSave(profile: Profile) {
        showToast(R.string.active_unsaved_tips, ToastDuration.Long) {
            setAction(R.string.edit) {
                requests.trySend(Request.Edit(profile))
            }
        }
    }

    fun updateElapsed() {
        adapter.updateElapsed()
    }

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        binding.navigation.configureMainNavigation(MainNavigationDestination.Profiles) {
            when (it) {
                MainNavigationDestination.Home -> requests.trySend(Request.OpenHome)
                MainNavigationDestination.Proxy -> requests.trySend(Request.OpenProxy)
                MainNavigationDestination.Profiles -> Unit
                MainNavigationDestination.Settings -> requests.trySend(Request.OpenSettings)
            }
        }

        binding.mainList.recyclerList.also {
            it.bindAppBarElevation(binding.activityBarLayout)
            it.applyLinearAdapter(context, adapter)
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.navigation.setProxyNavigationEnabled(running)
        }
    }

    fun showMenu(profile: Profile) {
        val dialog = AppBottomSheetDialog(context)

        val binding = DialogProfilesMenuBinding
            .inflate(context.layoutInflater, dialog.window?.decorView as ViewGroup?, false)

        binding.master = this
        binding.self = dialog
        binding.profile = profile

        dialog.setContentView(binding.root)
        dialog.show()
    }

    fun showCreateDialog(providers: List<ProfileProvider>) {
        val dialog = AppBottomSheetDialog(context)
        val binding = DialogProfileProvidersBinding.inflate(context.layoutInflater)
        val providerAdapter = ProfileProviderAdapter(
            context,
            select = { provider ->
                requests.trySend(Request.CreateProfile(provider))
                dialog.dismiss()
            },
            detail = { provider ->
                if (provider is ProfileProvider.External) {
                    requests.trySend(Request.OpenProviderDetail(provider))
                    dialog.dismiss()
                    true
                } else {
                    false
                }
            },
        ).apply {
            this.providers = providers
        }

        binding.providersList.applyLinearAdapter(context, providerAdapter)
        dialog.setContentView(binding.root)
        dialog.show()
    }

    fun showCreateUrlDialog() {
        val dialog = AppBottomSheetDialog(context)
        val binding = DialogCreateUrlProfileBinding.inflate(context.layoutInflater)

        binding.createButton.setOnClickListener {
            val name = binding.nameField.text?.toString()?.trim().orEmpty()
            val url = binding.urlField.text?.toString()?.trim().orEmpty()
            var valid = true

            if (name.isBlank()) {
                binding.nameLayout.error = context.getString(R.string.should_not_be_blank)
                valid = false
            } else {
                binding.nameLayout.error = null
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                binding.urlLayout.error = context.getString(R.string.accept_http_content)
                valid = false
            } else {
                binding.urlLayout.error = null
            }

            if (valid) {
                requests.trySend(Request.CreateUrl(name, url))
                dialog.dismiss()
            }
        }
        binding.cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(binding.root)
        dialog.show()
        binding.urlField.requestFocus()
    }

    suspend fun withProcessing(executeTask: suspend (suspend (FetchStatus) -> Unit) -> Unit) {
        context.withModelProgressBar {
            configure {
                isIndeterminate = true
                text = context.getString(R.string.initializing)
            }

            executeTask { status ->
                configure { applyFrom(status) }
            }
        }
    }

    private fun ModelProgressBarConfigure.applyFrom(status: FetchStatus) {
        when (status.action) {
            FetchStatus.Action.FetchConfiguration -> {
                text = context.getString(R.string.format_fetching_configuration, status.args[0])
                isIndeterminate = true
            }
            FetchStatus.Action.FetchProviders -> {
                text = context.getString(R.string.format_fetching_provider, status.args[0])
                isIndeterminate = false
                max = status.max
                progress = status.progress
            }
            FetchStatus.Action.SubscriptionInfo -> Unit
            FetchStatus.Action.Verifying -> {
                text = context.getString(R.string.verifying)
                isIndeterminate = false
                max = status.max
                progress = status.progress
            }
        }
    }

    fun requestUpdateAll() {
        allUpdating = true;
        changeUpdateAllButtonStatus()
        requests.trySend(Request.UpdateAll)
    }

    fun finishUpdateAll() {
        allUpdating = false;
        changeUpdateAllButtonStatus()
    }

    fun requestCreate() {
        requests.trySend(Request.Create)
    }

    private fun requestActive(profile: Profile) {
        requests.trySend(Request.Active(profile))
    }

    fun requestUpdate(dialog: Dialog, profile: Profile) {
        requests.trySend(Request.Update(profile))

        dialog.dismiss()
    }

    fun requestEdit(dialog: Dialog, profile: Profile) {
        requests.trySend(Request.Edit(profile))

        dialog.dismiss()
    }

    fun requestDuplicate(dialog: Dialog, profile: Profile) {
        requests.trySend(Request.Duplicate(profile))

        dialog.dismiss()
    }

    fun requestDelete(dialog: Dialog, profile: Profile) {
        requests.trySend(Request.Delete(profile))

        dialog.dismiss()
    }

    private fun changeUpdateAllButtonStatus() {
        if (allUpdating) {
            binding.updateView.startAnimation(rotateAnimation)
        } else {
            binding.updateView.clearAnimation()
        }
    }
}
