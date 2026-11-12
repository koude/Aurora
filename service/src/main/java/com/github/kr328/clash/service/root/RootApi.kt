package com.github.kr328.clash.service.root

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.net.HttpURLConnection
import java.net.URL

class RootApi(private val context: Context) {
    fun get(path: String): JsonObject = request("GET", path).asObject()
    fun put(path: String, body: String? = null) { request("PUT", path, body) }
    fun delete(path: String) { request("DELETE", path) }

    private fun request(method: String, path: String, body: String? = null): JsonElement {
        val secret = readSecret()
        val connection = URL("http://127.0.0.1:19090$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.setRequestProperty("Authorization", "Bearer $secret")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        try {
            val status = connection.responseCode
            check(status in 200..299) {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "ROOT core returned HTTP $status"
            }
            val response = connection.inputStream.bufferedReader().use { it.readLine().orEmpty() }
            return if (response.isBlank()) JsonNull else Json.parseToJsonElement(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun readSecret(): String = context.filesDir.resolve("aurora-root-secret")
        .takeIf { it.isFile }?.readText()?.trim().orEmpty()
        .also { check(it.isNotEmpty()) { "ROOT core is not running" } }

    private fun JsonElement.asObject(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
}
