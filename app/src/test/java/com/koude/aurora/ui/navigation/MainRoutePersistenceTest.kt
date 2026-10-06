package com.koude.aurora.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MainRoutePersistenceTest {
    @Test fun restoresEveryTopLevelRoute() {
        persistedMainRoutes.forEach { route -> assertEquals(route, initialMainRoute(route)) }
    }

    @Test fun childAndUnknownRoutesFallBackToHome() {
        assertEquals("home", initialMainRoute("profiles"))
        assertEquals("home", initialMainRoute("unknown"))
        assertEquals("home", initialMainRoute(null))
    }
}
