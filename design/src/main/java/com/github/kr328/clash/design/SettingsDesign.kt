package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignSettingsBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.MainNavigationDestination
import com.github.kr328.clash.design.util.configureMainNavigation
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.design.util.setProxyNavigationEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsDesign(context: Context) : Design<SettingsDesign.Request>(context) {
    enum class Request {
        StartApp, StartNetwork, StartOverride, StartMetaFeature,
        OpenHome, OpenProxy, OpenProfiles,
    }

    private val binding = DesignSettingsBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        binding.navigation.configureMainNavigation(MainNavigationDestination.Settings) {
            request(
                when (it) {
                    MainNavigationDestination.Home -> Request.OpenHome
                    MainNavigationDestination.Proxy -> Request.OpenProxy
                    MainNavigationDestination.Profiles -> Request.OpenProfiles
                    MainNavigationDestination.Settings -> return@configureMainNavigation
                }
            )
        }

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.navigation.setProxyNavigationEnabled(running)
        }
    }

    fun request(request: Request) {
        requests.trySend(request)
    }
}
