package com.github.kr328.clash.service

import android.app.NotificationManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.common.compat.getColorCompat
import com.github.kr328.clash.common.compat.startForegroundCompat
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.util.sendProfileUpdateCompleted
import com.github.kr328.clash.service.util.sendProfileUpdateFailed
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class ProfileWorker : BaseService() {
    private val service: ProfileWorker
        get() = this

    private val jobs = ConcurrentLinkedQueue<Job>()

    override fun onCreate() {
        super.onCreate()

        createChannels()

        foreground()

        launch {
            // Allow onStartCommand to enqueue work, then stop as soon as it finishes.
            delay(TimeUnit.SECONDS.toMillis(1))

            while (true) {
                jobs.poll()?.join() ?: break
            }

            stopSelf()
        }
    }

    override fun onDestroy() {
        stopForeground(true)

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            Intents.ACTION_PROFILE_REQUEST_UPDATE -> {
                intent.uuid?.also {
                    val job = launch {
                        run(it)
                    }

                    jobs.add(job)
                }
            }
            Intents.ACTION_PROFILE_SCHEDULE_UPDATES -> {
                val job = launch {
                    ProfileReceiver.rescheduleAll(service)
                }

                jobs.add(job)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun run(uuid: UUID) {
        val imported = ImportedDao().queryByUUID(uuid) ?: return

        try {
            ProfileProcessor.update(this, imported.uuid, null)

            sendProfileUpdateCompleted(imported.uuid)

            ProfileReceiver.scheduleNext(this, imported)
        } catch (e: Exception) {
            sendProfileUpdateFailed(imported.uuid, e.message ?: "Unknown")
        }
    }

    private fun createChannels() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(
                SERVICE_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(getString(R.string.profile_service_status)).build()
        )

        // Remove channels used by earlier versions for redundant progress/result notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.apply {
                deleteNotificationChannel(STATUS_CHANNEL)
                deleteNotificationChannel(RESULT_CHANNEL)
            }
        }
    }

    private fun foreground() {
        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL)
            .setContentTitle(getString(R.string.profile_updater))
            .setContentText(getString(R.string.profile_updating))
            .setColor(getColorCompat(R.color.aurora_notification_accent))
            .setSmallIcon(R.drawable.ic_aurora_status)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        startForegroundCompat(R.id.nf_profile_worker, notification)
    }

    companion object {
        private const val SERVICE_CHANNEL = "profile_service_channel"
        private const val STATUS_CHANNEL = "profile_status_channel"
        private const val RESULT_CHANNEL = "profile_result_channel"
    }

    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }
}
