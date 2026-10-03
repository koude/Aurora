package com.github.kr328.clash

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import com.github.kr328.clash.common.util.intent
import kotlin.reflect.KClass

internal fun Activity.navigateTopLevel(destination: KClass<out Activity>) {
    val route = when (destination) {
        MainActivity::class -> MainActivity.ROUTE_HOME
        ProxyActivity::class -> MainActivity.ROUTE_PROXY
        ProfilesActivity::class -> MainActivity.ROUTE_PROFILES
        SettingsActivity::class -> MainActivity.ROUTE_SETTINGS
        else -> null
    }
    val target = if (route == null) destination else MainActivity::class
    val intent = target.intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        route?.let { putExtra(MainActivity.EXTRA_TOP_LEVEL_ROUTE, it) }
        if (target == MainActivity::class) {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
    startActivity(intent, options.toBundle())
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
}
