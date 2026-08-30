package com.ateeb.onionpeel

import android.app.Application
import com.ateeb.onionpeel.companion.CompanionClient
import com.ateeb.onionpeel.companion.CompanionSyncService
import com.ateeb.onionpeel.data.PrefsRepository
import com.ateeb.onionpeel.policy.PeelModeController

class OnionpeelApp : Application() {
    lateinit var prefs: PrefsRepository
        private set

    lateinit var peelMode: PeelModeController
        private set

    lateinit var companionClient: CompanionClient
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsRepository(this)
        peelMode = PeelModeController(this, prefs)
        companionClient = CompanionClient(prefs)
        if (prefs.companionPaired) {
            CompanionSyncService.start(this)
        }
    }

    companion object {
        fun get(app: Application): OnionpeelApp = app as OnionpeelApp
    }
}
