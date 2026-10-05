package com.koude.aurora.data.home

import com.koude.aurora.model.WebsiteLatencySite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import android.os.SystemClock

interface WebsiteLatencyRepository {
    suspend fun measure(site: WebsiteLatencySite): Long?
}

class HttpWebsiteLatencyRepository : WebsiteLatencyRepository {
    override suspend fun measure(site: WebsiteLatencySite): Long? = withContext(Dispatchers.IO) {
        val connection = (URL(site.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 6_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            useCaches = false
            setRequestProperty("User-Agent", "Aurora connectivity check")
        }
        try {
            val startedAt = SystemClock.elapsedRealtime()
            connection.connect()
            connection.responseCode
            SystemClock.elapsedRealtime() - startedAt
        } catch (_: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private val WebsiteLatencySite.url: String
        get() = when (this) {
            WebsiteLatencySite.Apple -> "https://www.apple.com/library/test/success.html"
            WebsiteLatencySite.GitHub -> "https://github.com/"
            WebsiteLatencySite.YouTube -> "https://www.youtube.com/generate_204"
            WebsiteLatencySite.Google -> "https://www.google.com/generate_204"
        }
}

object WebsiteLatencyRepositoryProvider {
    val instance: WebsiteLatencyRepository by lazy { HttpWebsiteLatencyRepository() }
}
