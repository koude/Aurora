package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent

internal enum class TopLevelDestination(val route: String) {
    Home(MainActivity.ROUTE_HOME),
    Proxy(MainActivity.ROUTE_PROXY),
    Profiles(MainActivity.ROUTE_PROFILES),
    Settings(MainActivity.ROUTE_SETTINGS),
}

internal fun android.app.Activity.navigateTopLevel(destination: TopLevelDestination) {
    val intent = MainActivity::class.intent.apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
        addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(MainActivity.EXTRA_TOP_LEVEL_ROUTE, destination.route)
    }

    val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
    startActivity(intent, options.toBundle())
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
}
