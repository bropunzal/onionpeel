package com.ateeb.onionpeel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ateeb.onionpeel.OnionpeelApp
import com.ateeb.onionpeel.companion.CompanionSyncService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val app = OnionpeelApp.get(context.applicationContext as android.app.Application)
        app.peelMode.reapplyIfActive()
        if (app.prefs.companionPaired) {
            CompanionSyncService.start(context)
        }
    }
}
