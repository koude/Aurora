package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.SettingsDesign
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class SettingsActivity : BaseActivity<SettingsDesign>() {
    override suspend fun main() {
        val design = SettingsDesign(this)

        setContentDesign(design)

        design.setClashRunning(clashRunning)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart, Event.ClashStop ->
                            design.setClashRunning(clashRunning)
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        SettingsDesign.Request.OpenHome ->
                            navigateTopLevel(MainActivity::class)
                        SettingsDesign.Request.OpenProxy ->
                            navigateTopLevel(ProxyActivity::class)
                        SettingsDesign.Request.OpenProfiles ->
                            navigateTopLevel(ProfilesActivity::class)
                        SettingsDesign.Request.StartApp ->
                            startActivity(AppSettingsActivity::class.intent)
                        SettingsDesign.Request.StartNetwork ->
                            startActivity(NetworkSettingsActivity::class.intent)
                        SettingsDesign.Request.StartOverride ->
                            startActivity(OverrideSettingsActivity::class.intent)
                        SettingsDesign.Request.StartMetaFeature ->
                            startActivity(MetaFeatureSettingsActivity::class.intent)
                    }
                }
            }
        }
    }
}
