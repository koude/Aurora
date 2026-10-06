package com.koude.aurora.data.connections

import android.content.Context
import com.github.kr328.clash.core.model.ConnectionInfo
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConnectionsSnapshot(
    val connections: List<ConnectionInfo>,
    val appLabels: Map<String, String>,
)

interface ConnectionsRepository {
    suspend fun query(): ConnectionsSnapshot
    suspend fun close(ids: List<String>)
}

class ServiceConnectionsRepository(context: Context) : ConnectionsRepository {
    private val packageManager = context.applicationContext.packageManager
    private val appLabelResolver = AppLabelResolver { packageName ->
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }

    override suspend fun query(): ConnectionsSnapshot {
        val connections = withClash { queryConnections().toList() }
        val appLabels = withContext(Dispatchers.IO) {
            connections.map(ConnectionInfo::process)
                .filter(String::isNotBlank)
                .distinct()
                .mapNotNull { packageName ->
                    appLabelResolver.resolve(packageName)?.let { packageName to it }
                }
                .toMap()
        }
        return ConnectionsSnapshot(connections, appLabels)
    }

    override suspend fun close(ids: List<String>) {
        withClash { ids.forEach { id -> closeConnection(id) } }
    }
}
