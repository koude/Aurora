package com.koude.aurora.ui.proxy

import org.junit.Assert.assertEquals
import org.junit.Test

class ProxyGroupExpansionTest {
    @Test fun openingAnotherGroupClosesThePreviousGroup() {
        assertEquals(mapOf("B" to true), expandedProxyGroup("B", true))
    }

    @Test fun collapsingGroupClosesAllGroups() {
        assertEquals(emptyMap<String, Boolean>(), expandedProxyGroup("A", false))
    }

    @Test fun backgroundKeepsExpansionButNavigationClearsIt() {
        val expanded = expandedProxyGroup("A", true)
        assertEquals(expanded, expandedProxyGroupsForRoute("proxy", "proxy", expanded))
        assertEquals(emptyMap<String, Boolean>(), expandedProxyGroupsForRoute("home", "proxy", expanded))
    }
}
