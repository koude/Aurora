package com.koude.aurora.ui.navigation

internal val persistedMainRoutes = setOf("home", "proxy", "connections", "settings")

internal fun initialMainRoute(savedRoute: String?): String =
    savedRoute?.takeIf { it in persistedMainRoutes } ?: "home"
