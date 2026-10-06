package com.koude.aurora.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverridePortTest {
    @Test fun acceptsDisabledAndValidPorts() {
        assertEquals(0, parsePortOverrideInput(""))
        assertEquals(0, parsePortOverrideInput(" 0 "))
        assertEquals(7890, parsePortOverrideInput(" 7890 "))
        assertEquals(65535, parsePortOverrideInput("65535"))
    }

    @Test fun rejectsInvalidPorts() {
        assertNull(parsePortOverrideInput("abc"))
        assertNull(parsePortOverrideInput("-1"))
        assertNull(parsePortOverrideInput("65536"))
        assertNull(parsePortOverrideInput("999999999999999999999"))
    }
}
