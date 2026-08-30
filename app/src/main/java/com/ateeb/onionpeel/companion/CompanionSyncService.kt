package com.ateeb.onionpeel.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ateeb.onionpeel.OnionpeelApp
import com.ateeb.onionpeel.R
import com.ateeb.onionpeel.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls the desktop companion for peel state. Peel on/off is never decided on the phone.
 */
class CompanionSyncService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Syncing with desktop…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loopJob?.cancel()
        loopJob = scope.launch {
            val app = OnionpeelApp.get(application)
            val client = app.companionClient
            val controller = app.peelMode
            while (isActive) {
                if (app.prefs.companionPaired && controller.isDeviceOwner()) {
                    client.syncPeelDesired()
                        .onSuccess { desired ->
                            val active = app.prefs.peelModeActive
                            if (desired && !active && app.prefs.setupCompleted) {
                                controller.enablePeelMode()
                            } else if (!desired && active) {
                                controller.disablePeelMode()
                            }
                            client.reportPeelState(app.prefs.peelModeActive)
                            updateNotification(
                                if (app.prefs.peelModeActive) "Peeled · desktop controls peel"
                                else "Open · desktop controls peel",
                            )
                        }
                        .onFailure {
                            updateNotification("Cannot reach desktop companion")
                        }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Onionpeel")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Desktop companion",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "companion_sync"
        private const val NOTIFICATION_ID = 42
        private const val POLL_INTERVAL_MS = 15_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CompanionSyncService::class.java))
        }
    }
}
