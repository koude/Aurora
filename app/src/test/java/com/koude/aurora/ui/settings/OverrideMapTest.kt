package com.koude.aurora.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class OverrideMapTest {
    @Test fun parsesEntriesAndKeepsEqualsInValue() {
        assertEquals(
            OverrideMapInput.Valid(mapOf("example.com" to "1.2.3.4", "api.example" to "https://dns.example/query?a=b")),
            parseOverrideMapInput("example.com=1.2.3.4\n\n api.example = https://dns.example/query?a=b"),
        )
        assertEquals(OverrideMapInput.Valid(emptyMap()), parseOverrideMapInput(" \n"))
    }

    @Test fun rejectsMalformedOrDuplicateEntriesAtTheirLine() {
        assertEquals(OverrideMapInput.Invalid(2), parseOverrideMapInput("good=value\nmissing separator"))
        assertEquals(OverrideMapInput.Invalid(2), parseOverrideMapInput("good=value\ngood=other"))
        assertEquals(OverrideMapInput.Invalid(1), parseOverrideMapInput("=value"))
        assertEquals(OverrideMapInput.Invalid(1), parseOverrideMapInput("key="))
    }
}
