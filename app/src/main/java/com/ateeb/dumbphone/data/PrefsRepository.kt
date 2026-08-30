package com.ateeb.dumbphone.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PrefsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var dumbModeActive: Boolean
        get() = prefs.getBoolean(KEY_DUMB_ACTIVE, false)
        set(value) = prefs.edit { putBoolean(KEY_DUMB_ACTIVE, value) }

    var exitRequestedAtMillis: Long
        get() = prefs.getLong(KEY_EXIT_REQUESTED_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_EXIT_REQUESTED_AT, value) }

    var exitDelayHours: Int
        get() = prefs.getInt(KEY_EXIT_DELAY_HOURS, DEFAULT_EXIT_DELAY_HOURS)
        set(value) = prefs.edit { putInt(KEY_EXIT_DELAY_HOURS, value.coerceIn(1, 168)) }

    var emergencyPhrase: String
        get() = prefs.getString(KEY_EMERGENCY_PHRASE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_EMERGENCY_PHRASE, value) }

    var emergencyMonthKey: String
        get() = prefs.getString(KEY_EMERGENCY_MONTH, "") ?: ""
        set(value) = prefs.edit { putString(KEY_EMERGENCY_MONTH, value) }

    var emergencyUsesThisMonth: Int
        get() = prefs.getInt(KEY_EMERGENCY_USES, 0)
        set(value) = prefs.edit { putInt(KEY_EMERGENCY_USES, value) }

    var setupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_SETUP_DONE, value) }

    fun getAllowList(): Set<String> {
        return prefs.getStringSet(KEY_ALLOW_LIST, null) ?: DefaultAllowList.PACKAGES
    }

    fun setAllowList(packages: Set<String>) {
        prefs.edit { putStringSet(KEY_ALLOW_LIST, packages.toSet()) }
    }

    fun clearExitRequest() {
        prefs.edit { remove(KEY_EXIT_REQUESTED_AT) }
    }

    companion object {
        private const val PREFS_NAME = "dumbphone_prefs"
        private const val KEY_DUMB_ACTIVE = "dumb_active"
        private const val KEY_EXIT_REQUESTED_AT = "exit_requested_at"
        private const val KEY_EXIT_DELAY_HOURS = "exit_delay_hours"
        private const val KEY_EMERGENCY_PHRASE = "emergency_phrase"
        private const val KEY_EMERGENCY_MONTH = "emergency_month"
        private const val KEY_EMERGENCY_USES = "emergency_uses"
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_ALLOW_LIST = "allow_list"

        const val DEFAULT_EXIT_DELAY_HOURS = 24
        const val MAX_EMERGENCY_PER_MONTH = 2
        const val EMERGENCY_HOLD_SECONDS = 60
    }
}

object DefaultAllowList {
    /**
     * Suggested tool apps. User edits this list in setup.
     * Launcher and this app are always kept regardless of this list.
     */
    val PACKAGES: Set<String> = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.google.android.apps.maps",
        "com.google.android.calendar",
        "com.android.camera2",
        "com.google.android.GoogleCamera",
    )
}
