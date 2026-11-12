package com.github.kr328.clash.service.root

import android.content.Context
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.*
import com.github.kr328.clash.service.ClashManager
import com.github.kr328.clash.service.data.Selection
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.service.remote.IClashManager
import com.github.kr328.clash.service.remote.ILogObserver
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.sendOverrideChanged
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

/** Adapts the actual ROOT core's local controller to Aurora's existing UI. */
class RootClashManager(private val context: Context, private val fallback: ClashManager) : IClashManager by fallback {
    private val api = RootApi(context)
    private var logConnection: HttpURLConnection? = null
    private var logThread: Thread? = null

    override fun queryTunnelState(): TunnelState {
        val mode = api.get("/configs").text("mode").lowercase()
        return TunnelState(when (mode) {
            "global" -> TunnelState.Mode.Global
            "direct" -> TunnelState.Mode.Direct
            else -> TunnelState.Mode.Rule
        })
    }

    override fun queryTrafficNow(): Long {
        val response = api.get("/traffic")
        return traffic(response.long("up"), response.long("down"))
    }

    override fun queryTrafficTotal(): Long {
        val response = api.get("/connections")
        return traffic(response.long("uploadTotal"), response.long("downloadTotal"))
    }

    override fun queryRoutePreview(target: String): RoutePreview = RoutePreview(
        target = target,
        mode = queryTunnelState().mode.name.lowercase(),
        rule = "",
        policy = "",
        outbound = "",
        error = "ROOT 模式暂不支持规则预判",
    )

    override fun queryConnections(): ConnectionList {
        val list = api.get("/connections").array("connections")
        return ConnectionList(list.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val meta = item.obj("metadata")
            ConnectionInfo(
                id = item.text("id"),
                host = meta.text("host"),
                process = meta.text("process"),
                network = meta.text("network"),
                destination = listOf(meta.text("destinationIP"), meta.text("destinationPort"))
                    .filter(String::isNotBlank).joinToString(":"),
                rule = item.text("rule"),
                rulePayload = item.text("rulePayload"),
                chains = item.array("chains").mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
                uploaded = item.long("upload"),
                downloaded = item.long("download"),
            )
        })
    }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        val all = api.get("/proxies").obj("proxies")
        // GLOBAL.all follows the profile order; JSON object keys do not.
        val orderedNames = all.obj("GLOBAL").array("all")
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .ifEmpty { all.keys.toList() }
        return orderedNames.filter { name ->
            val proxy = all[name] as? JsonObject ?: return@filter false
            name != "GLOBAL" && proxy.array("all").isNotEmpty() &&
                (!excludeNotSelectable || proxy.text("type") == "Selector") &&
                proxy.text("hidden") != "true"
        }
    }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        val all = api.get("/proxies").obj("proxies")
        val group = all[name] as? JsonObject ?: error("Proxy group $name is unavailable")
        val members = group.array("all").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .map { member ->
                val item = all[member] as? JsonObject ?: JsonObject(emptyMap())
                val history = item.array("history").lastOrNull() as? JsonObject
                Proxy(
                    name = member,
                    title = member,
                    subtitle = item.text("type"),
                    type = item.text("type"),
                    delay = history?.int("delay") ?: 0,
                    isGroup = item.array("all").isNotEmpty(),
                )
            }
        val sorted = when (proxySort) {
            ProxySort.Title -> members.sortedBy { it.title }
            ProxySort.Delay -> members.sortedBy { it.delay }
            ProxySort.Default -> members
        }
        return ProxyGroup(group.text("type"), sorted, group.text("now"))
    }

    override fun queryConfiguration(): UiConfiguration = UiConfiguration()

    override fun queryProviders(): ProviderList {
        val result = mutableListOf<Provider>()
        for ((path, type) in listOf("proxies" to Provider.Type.Proxy, "rules" to Provider.Type.Rule)) {
            val providers = api.get("/providers/$path").obj("providers")
            providers.forEach { (name, element) ->
                val item = element as? JsonObject ?: return@forEach
                val vehicle = runCatching { Provider.VehicleType.valueOf(item.text("vehicleType")) }
                    .getOrDefault(Provider.VehicleType.Compatible)
                result += Provider(name, type, vehicle, 0)
            }
        }
        return ProviderList(result)
    }

    override fun patchSelector(group: String, name: String): Boolean = runCatching {
        api.put("/proxies/${segment(group)}", "{\"name\":${JsonPrimitive(name)}}")
        ServiceStore(context).activeProfile?.let { SelectionDao().setSelected(Selection(it, group, name)) }
        true
    }.getOrDefault(false)

    override fun closeConnection(id: String): Boolean = runCatching {
        api.delete("/connections/${segment(id)}")
        true
    }.getOrDefault(false)

    override fun closeAllConnections() { api.delete("/connections") }

    override suspend fun healthCheck(group: String) {
        api.get("/group/${segment(group)}/delay?url=https%3A%2F%2Fwww.gstatic.com%2Fgenerate_204&timeout=5000")
    }

    override suspend fun testProxy(name: String) {
        api.get("/proxies/${segment(name)}/delay?url=https%3A%2F%2Fwww.gstatic.com%2Fgenerate_204&timeout=5000")
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        val path = if (type == Provider.Type.Proxy) "proxies" else "rules"
        api.put("/providers/$path/${segment(name)}")
    }

    override fun patchOverride(slot: Clash.OverrideSlot, configuration: ConfigurationOverride) {
        fallback.patchOverride(slot, configuration)
    }

    override fun clearOverride(slot: Clash.OverrideSlot) {
        fallback.clearOverride(slot)
        context.sendOverrideChanged()
    }

    @Synchronized
    override fun setLogObserver(observer: ILogObserver?) {
        logConnection?.disconnect()
        logThread?.interrupt()
        logConnection = null
        logThread = null
        if (observer == null) return

        logThread = Thread({
            val key = context.filesDir.resolve("aurora-root-secret")
                .takeIf { it.isFile }?.readText()?.trim().orEmpty()
            if (key.isEmpty()) return@Thread
            val connection = URL("http://127.0.0.1:19090/logs?level=debug")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 0
            connection.setRequestProperty("Authorization", "Bearer $key")
            synchronized(this) { logConnection = connection }
            try {
                connection.inputStream.bufferedReader().useLines { lines ->
                    lines.takeWhile { !Thread.currentThread().isInterrupted }.forEach { line ->
                        val item = Json.parseToJsonElement(line) as? JsonObject ?: return@forEach
                        val level = when (item.text("type")) {
                            "debug" -> LogMessage.Level.Debug
                            "info" -> LogMessage.Level.Info
                            "warning" -> LogMessage.Level.Warning
                            "error" -> LogMessage.Level.Error
                            else -> LogMessage.Level.Unknown
                        }
                        observer.newItem(LogMessage(level, item.text("payload"), Date()))
                    }
                }
            } catch (_: Exception) {
                // Closing the observer disconnects the stream.
            } finally {
                connection.disconnect()
                synchronized(this) { if (logConnection === connection) logConnection = null }
            }
        }, "AuroraRootLogs").apply { isDaemon = true; start() }
    }

    private fun traffic(upload: Long, download: Long): Long =
        (amount(upload) shl 32) or amount(download)

    private fun amount(bytes: Long): Long {
        var value = bytes.coerceAtLeast(0)
        var unit = 0L
        while (value > 0x3fffffff && unit < 3) {
            value /= 1024
            unit++
        }
        return (unit shl 30) or value.coerceAtMost(0x3fffffff)
    }

    private fun segment(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.int(key: String): Int = (this[key] as? JsonPrimitive)?.intOrNull ?: 0
    private fun JsonObject.long(key: String): Long = (this[key] as? JsonPrimitive)?.longOrNull ?: 0L
    private fun JsonObject.obj(key: String): JsonObject = this[key] as? JsonObject ?: JsonObject(emptyMap())
    private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())
}
