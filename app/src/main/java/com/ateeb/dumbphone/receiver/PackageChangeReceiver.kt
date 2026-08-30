package com.ateeb.dumbphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ateeb.dumbphone.DumbphoneApp

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_PACKAGE_ADDED &&
            intent?.action != Intent.ACTION_PACKAGE_REPLACED
        ) {
            return
        }
        val app = DumbphoneApp.get(context.applicationContext as android.app.Application)
        app.dumbMode.reapplyIfActive()
    }
}
