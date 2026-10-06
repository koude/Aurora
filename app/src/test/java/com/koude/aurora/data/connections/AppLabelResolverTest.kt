package com.koude.aurora.data.connections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLabelResolverTest {
    @Test
    fun resolvesAndCachesConfirmedLabels() {
        var lookups = 0
        val resolver = AppLabelResolver {
            lookups++
            if (it == "com.example.browser") " Browser " else null
        }

        assertEquals("Browser", resolver.resolve("com.example.browser"))
        assertEquals("Browser", resolver.resolve("com.example.browser"))
        assertEquals(1, lookups)
    }

    @Test
    fun unknownPackagesStayUnknownWithoutRepeatedLookup() {
        var lookups = 0
        val resolver = AppLabelResolver {
            lookups++
            throw IllegalArgumentException("No such package")
        }

        assertNull(resolver.resolve(""))
        assertNull(resolver.resolve("shared.uid"))
        assertNull(resolver.resolve("shared.uid"))
        assertEquals(1, lookups)
    }
}
