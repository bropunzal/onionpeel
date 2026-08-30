package com.ateeb.onionpeel.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
 * Polls the desktop companion for peel state and policy. Peel on/off is never decided on the phone.
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
                    client.syncFromDesktop()
                        .onSuccess { sync ->
                            applyPolicy(app, sync)
                            val active = app.prefs.peelModeActive
                            if (sync.peelDesired && !active && app.prefs.setupCompleted) {
                                controller.enablePeelMode()
                            } else if (!sync.peelDesired && active) {
                                controller.disablePeelMode()
                            } else if (sync.peelDesired && active) {
                                controller.reapplyIfActive()
                            }
                            client.reportPeelState(
                                active = app.prefs.peelModeActive,
                                apps = loadLaunchableApps(app),
                            )
                            updateNotification(notificationText(app, sync))
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

    private fun applyPolicy(app: OnionpeelApp, sync: CompanionSyncPayload) {
        val prefs = app.prefs
        if (sync.blockedUrls.isNotEmpty()) {
            prefs.setBlockedUrls(sync.blockedUrls)
        }
        if (sync.allowList.isNotEmpty()) {
            prefs.setAllowList(sync.allowList)
        }
        prefs.exitDelayHours = sync.exitDelayHours
        if (!prefs.setupCompleted && prefs.companionPaired) {
            prefs.setupCompleted = true
        }
    }

    private fun notificationText(app: OnionpeelApp, sync: CompanionSyncPayload): String {
        if (sync.unpeelAt != null && sync.unpeelAt > System.currentTimeMillis()) {
            val hoursLeft = ((sync.unpeelAt - System.currentTimeMillis()) / 3_600_000.0)
            return "Unpeel in ${"%.1f".format(hoursLeft)}h · desktop controls peel"
        }
        return if (app.prefs.peelModeActive) {
            "Peeled · desktop controls peel"
        } else {
            "Open · desktop controls peel"
        }
    }

    private fun loadLaunchableApps(app: OnionpeelApp): List<ReportedApp> {
        val pm = application.packageManager
        val allow = app.prefs.getAllowList()
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == packageName) return@mapNotNull null
                ReportedApp(
                    packageName = pkg,
                    label = resolve.loadLabel(pm).toString(),
                    allowed = pkg in allow,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

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
            .setContentTitle("OnionPeel")
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
