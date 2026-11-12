package com.github.kr328.clash.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.service.clash.clashRuntime
import com.github.kr328.clash.service.clash.module.CloseModule
import com.github.kr328.clash.service.clash.module.RootRefreshModule
import com.github.kr328.clash.service.clash.module.StaticNotificationModule
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.service.root.RootCore
import com.github.kr328.clash.service.root.RootApi
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.sendClashStarted
import com.github.kr328.clash.service.util.sendClashStopped
import com.github.kr328.clash.service.util.sendProfileLoaded
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class)
class RootService : BaseService() {
    private val core by lazy { RootCore(this) }
    private var reason: String? = null

    private val runtime = clashRuntime {
        val close = install(CloseModule(this@RootService))
        val refresh = install(RootRefreshModule(this@RootService))
        install(StaticNotificationModule(this@RootService))

        try {
            rootLoop@ while (isActive) {
                val uuid = ServiceStore(this@RootService).activeProfile
                    ?: error("No profile selected")
                val imported = ImportedDao().queryByUUID(uuid)
                    ?: error("Selected profile is not imported")
                core.start(imported)
                val api = RootApi(this@RootService)
                SelectionDao().querySelections(uuid).forEach { selection ->
                    val group = URLEncoder.encode(selection.proxy, "UTF-8").replace("+", "%20")
                    runCatching {
                        api.put("/proxies/$group", "{\"name\":${JsonPrimitive(selection.selected)}}")
                    }
                }
                StatusProvider.currentProfile = imported.name
                sendProfileLoaded(uuid)
                sendClashStarted()

                while (isActive) {
                    when (select<Int> {
                        close.onEvent { 0 }
                        refresh.onEvent { 1 }
                        onTimeout(5000) { 2 }
                    }) {
                        0 -> break@rootLoop
                        1 -> {
                            StatusProvider.currentProfile = null
                            sendClashStopped(null)
                            break
                        }
                        else -> if (!core.isHealthy()) error("ROOT core stopped unexpectedly")
                    }
                }
                core.stop()
            }
        } catch (e: Exception) {
            reason = e.message ?: "ROOT mode failed"
            Log.e("ROOT mode: $reason", e)
        } finally {
            withContext(NonCancellable) {
                runCatching { core.stop() }.onFailure { Log.e("Unable to stop ROOT core", it) }
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (StatusProvider.serviceRunning) return stopSelf()
        StatusProvider.serviceRunning = true
        StatusProvider.currentProfile = null
        StaticNotificationModule.createNotificationChannel(this)
        StaticNotificationModule.notifyLoadingNotification(this)
        runtime.launch()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder = Binder()

    override fun onDestroy() {
        StatusProvider.serviceRunning = false
        StatusProvider.currentProfile = null
        sendClashStopped(reason)
        super.onDestroy()
    }
}
