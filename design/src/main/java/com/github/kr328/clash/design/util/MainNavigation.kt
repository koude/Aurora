package com.github.kr328.clash.design.util

import com.github.kr328.clash.design.R
import com.google.android.material.bottomnavigation.BottomNavigationView

enum class MainNavigationDestination(val itemId: Int) {
    Home(R.id.navigation_home),
    Proxy(R.id.navigation_proxy),
    Profiles(R.id.navigation_profiles),
    Settings(R.id.navigation_settings),
}

fun BottomNavigationView.configureMainNavigation(
    selected: MainNavigationDestination,
    navigate: (MainNavigationDestination) -> Unit,
) {
    selectedItemId = selected.itemId
    setOnItemSelectedListener { item ->
        val destination = MainNavigationDestination.entries
            .firstOrNull { it.itemId == item.itemId }
            ?: return@setOnItemSelectedListener false

        if (destination == selected) {
            true
        } else {
            navigate(destination)
            false
        }
    }
}

fun BottomNavigationView.setProxyNavigationEnabled(enabled: Boolean) {
    menu.findItem(R.id.navigation_proxy).isEnabled = enabled
}
