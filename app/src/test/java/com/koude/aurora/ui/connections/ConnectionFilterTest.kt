package com.koude.aurora.ui.connections

import com.github.kr328.clash.core.model.ConnectionInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionFilterTest {
    private val connections = listOf(
        ConnectionInfo(id = "github", host = "github.com", process = "Browser"),
        ConnectionInfo(id = "video", host = "youtube.com", chains = listOf("Video Proxy")),
        ConnectionInfo(id = "ip", destination = "203.0.113.7:443", rule = "MATCH"),
    )

    @Test
    fun blankSearchIncludesEveryDisplayedConnection() {
        assertEquals(connections.map(ConnectionInfo::id), filterConnections(connections, "  ").map(ConnectionInfo::id))
    }

    @Test
    fun searchIncludesOnlyMatchingConnections() {
        assertEquals(listOf("github"), filterConnections(connections, " GITHUB ").map(ConnectionInfo::id))
        assertEquals(listOf("video"), filterConnections(connections, "video proxy").map(ConnectionInfo::id))
        assertEquals(listOf("ip"), filterConnections(connections, "203.0.113.7").map(ConnectionInfo::id))
        assertEquals(emptyList<String>(), filterConnections(connections, "unmatched").map(ConnectionInfo::id))
    }
}
