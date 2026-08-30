package com.ateeb.onionpeel.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ateeb.onionpeel.OnionpeelApp
import com.ateeb.onionpeel.R

class OnionpeelDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.device_admin_enabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        val app = OnionpeelApp.get(context.applicationContext as android.app.Application)
        app.prefs.peelModeActive = false
        app.prefs.clearExitRequest()
        Toast.makeText(context, R.string.device_admin_disabled, Toast.LENGTH_LONG).show()
    }
}
