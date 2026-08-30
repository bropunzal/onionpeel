package com.ateeb.dumbphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ateeb.dumbphone.DumbphoneApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val app = DumbphoneApp.get(context.applicationContext as android.app.Application)
        app.dumbMode.reapplyIfActive()
    }
}
