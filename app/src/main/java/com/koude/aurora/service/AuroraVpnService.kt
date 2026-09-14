package com.koude.aurora.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.koude.aurora.MainActivity
import com.koude.aurora.R
import com.koude.aurora.core.MihomoCore
import com.koude.aurora.data.AppState
import com.koude.aurora.data.ConnectionState
import io.github.oviron.libmihomo.TunInterface
import java.util.concurrent.Executors

class AuroraVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val core by lazy { MihomoCore(applicationContext) }

    private val tunCallbacks = object : TunInterface {
        override fun protect(fd: Int) {
            this@AuroraVpnService.protect(fd)
        }

        override fun resolverProcess(
            protocol: Int,
            source: String,
            target: String,
            uid: Int
        ): String = ""
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> disconnect()
            else -> worker.execute {
                runCatching { connect() }.onFailure {
                    fail(it.message ?: it.javaClass.simpleName ?: "VPN 服务异常")
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun connect() {
        if (AppState.connection.value == ConnectionState.CONNECTED) return

        createChannel()
        startForeground(NOTIFICATION_ID, notification("正在准备连接"))
        AppState.setConnection(ConnectionState.CONNECTING, "正在加载 mihomo 核心")

        val config = filesDir.resolve("config.yaml")
        if (!config.exists()) {
            fail("请先导入配置文件")
            return
        }

        if (!core.isAvailable) {
            fail("Aurora 核心加载失败")
            return
        }

        AppState.setConnection(ConnectionState.CONNECTING, "正在创建 VPN 接口")

        val fd = Builder()
            .setSession("Aurora")
            .setMtu(1400)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .establish()

        if (fd == null) {
            fail("无法创建 VPN 接口")
            return
        }

        tun = fd
        AppState.setConnection(ConnectionState.CONNECTING, "正在启动 mihomo TUN")
        core.start(config.absolutePath, fd.fd, tunCallbacks)
            .onSuccess {
                AppState.setConnection(ConnectionState.CONNECTED)
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, notification("已连接"))
                AuroraTileService.requestRefresh(this)
            }
            .onFailure {
                fail(it.message ?: "核心启动失败")
            }
    }

    private fun fail(message: String) {
        runCatching { core.stop() }
        runCatching { tun?.close() }
        tun = null
        AppState.setConnection(ConnectionState.ERROR, message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        AuroraTileService.requestRefresh(this)
    }

    private fun disconnect() {
        worker.execute {
            runCatching { core.stop() }
            runCatching { tun?.close() }
            tun = null
            AppState.setConnection(ConnectionState.DISCONNECTED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            AuroraTileService.requestRefresh(this)
        }
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        runCatching { core.stop() }
        runCatching { tun?.close() }
        tun = null
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Aurora connection", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AuroraVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aurora)
            .setContentTitle("Aurora")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .addAction(0, "断开", stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "aurora_connection"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.koude.aurora.START"
        private const val ACTION_STOP = "com.koude.aurora.STOP"

        fun start(context: Context) {
            val intent = Intent(context, AuroraVpnService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AuroraVpnService::class.java).setAction(ACTION_STOP)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
