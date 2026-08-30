package com.ateeb.onionpeel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ateeb.onionpeel.OnionpeelApp

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_PACKAGE_ADDED &&
            intent?.action != Intent.ACTION_PACKAGE_REPLACED
        ) {
            return
        }
        val app = OnionpeelApp.get(context.applicationContext as android.app.Application)
        app.peelMode.reapplyIfActive()
    }
}
