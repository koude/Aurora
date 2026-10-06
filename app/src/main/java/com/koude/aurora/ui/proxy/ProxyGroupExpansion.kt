package com.koude.aurora.ui.proxy

/** Only one group may be expanded while the Proxy page remains active. */
internal fun expandedProxyGroup(name: String, expanded: Boolean): Map<String, Boolean> =
    if (expanded) mapOf(name to true) else emptyMap()

internal fun expandedProxyGroupsForRoute(
    route: String,
    proxyRoute: String,
    expandedGroups: Map<String, Boolean>,
): Map<String, Boolean> = if (route == proxyRoute) expandedGroups else emptyMap()
