package com.ateeb.dumbphone

import android.app.Application
import com.ateeb.dumbphone.data.PrefsRepository
import com.ateeb.dumbphone.policy.DumbModeController

class DumbphoneApp : Application() {
    lateinit var prefs: PrefsRepository
        private set

    lateinit var dumbMode: DumbModeController
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsRepository(this)
        dumbMode = DumbModeController(this, prefs)
    }

    companion object {
        fun get(app: Application): DumbphoneApp = app as DumbphoneApp
    }
}
