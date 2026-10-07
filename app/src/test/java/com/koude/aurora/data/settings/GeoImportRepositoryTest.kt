package com.koude.aurora.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoImportRepositoryTest {
    @Test fun supportedNamesMapToExistingCoreFileNames() {
        assertEquals("geoip.metadb", geoTargetFileName("geoip", "GeoIP.metadb"))
        assertEquals("geosite.dat", geoTargetFileName("geosite", "sites.dat"))
        assertEquals("country.mmdb", geoTargetFileName("country", "country.mmdb"))
        assertEquals("ASN.db", geoTargetFileName("asn", "asn.db"))
    }

    @Test fun unsupportedExtensionOrKindDoesNotProduceDestination() {
        assertNull(geoTargetFileName("geoip", "image.png"))
        assertNull(geoTargetFileName("unknown", "database.dat"))
    }
}
