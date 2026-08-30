package com.ateeb.dumbphone.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ateeb.dumbphone.DumbphoneApp
import com.ateeb.dumbphone.R

class DumbphoneDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.device_admin_enabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        val app = DumbphoneApp.get(context.applicationContext as android.app.Application)
        app.prefs.dumbModeActive = false
        app.prefs.clearExitRequest()
        Toast.makeText(context, R.string.device_admin_disabled, Toast.LENGTH_LONG).show()
    }
}
