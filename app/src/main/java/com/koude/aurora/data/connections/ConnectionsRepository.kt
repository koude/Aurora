package com.koude.aurora.data.connections

import com.github.kr328.clash.core.model.ConnectionInfo
import com.github.kr328.clash.util.withClash

data class ConnectionsSnapshot(
    val connections: List<ConnectionInfo>,
    val appLabels: Map<String, String>,
)

interface ConnectionsRepository {
    suspend fun query(): ConnectionsSnapshot
    suspend fun close(ids: List<String>)
}

class ServiceConnectionsRepository : ConnectionsRepository {
    override suspend fun query(): ConnectionsSnapshot {
        val connections = withClash { queryConnections().toList() }
        return ConnectionsSnapshot(connections, emptyMap())
    }

    override suspend fun close(ids: List<String>) {
        withClash { ids.forEach { id -> closeConnection(id) } }
    }
}
