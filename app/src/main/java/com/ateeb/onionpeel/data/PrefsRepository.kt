package com.ateeb.onionpeel.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PrefsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var peelModeActive: Boolean
        get() = prefs.getBoolean(KEY_PEEL_ACTIVE, false)
        set(value) = prefs.edit { putBoolean(KEY_PEEL_ACTIVE, value) }

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

    fun getBlockedUrls(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_URLS, null) ?: DefaultBlockedUrls.DOMAINS
    }

    fun setBlockedUrls(urls: Set<String>) {
        prefs.edit {
            putStringSet(
                KEY_BLOCKED_URLS,
                urls.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
            )
        }
    }

    fun addBlockedUrl(url: String) {
        setBlockedUrls(getBlockedUrls() + url.trim().lowercase())
    }

    fun removeBlockedUrl(url: String) {
        setBlockedUrls(getBlockedUrls() - url.trim().lowercase())
    }

    var companionBaseUrl: String
        get() = prefs.getString(KEY_COMPANION_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_COMPANION_URL, value.trim().removeSuffix("/")) }

    var companionToken: String
        get() = prefs.getString(KEY_COMPANION_TOKEN, "") ?: ""
        set(value) = prefs.edit { putString(KEY_COMPANION_TOKEN, value.trim()) }

    val companionPaired: Boolean
        get() = companionBaseUrl.isNotEmpty() && companionToken.isNotEmpty()

    var lastCompanionSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC, value) }

    var lastCompanionError: String
        get() = prefs.getString(KEY_LAST_SYNC_ERROR, "") ?: ""
        set(value) = prefs.edit { putString(KEY_LAST_SYNC_ERROR, value) }

    fun clearCompanion() {
        prefs.edit {
            remove(KEY_COMPANION_URL)
            remove(KEY_COMPANION_TOKEN)
            remove(KEY_LAST_SYNC)
            remove(KEY_LAST_SYNC_ERROR)
        }
    }

    fun clearExitRequest() {
        prefs.edit { remove(KEY_EXIT_REQUESTED_AT) }
    }

    companion object {
        private const val PREFS_NAME = "onionpeel_prefs"
        private const val KEY_PEEL_ACTIVE = "peel_active"
        private const val KEY_EXIT_REQUESTED_AT = "exit_requested_at"
        private const val KEY_EXIT_DELAY_HOURS = "exit_delay_hours"
        private const val KEY_EMERGENCY_PHRASE = "emergency_phrase"
        private const val KEY_EMERGENCY_MONTH = "emergency_month"
        private const val KEY_EMERGENCY_USES = "emergency_uses"
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_ALLOW_LIST = "allow_list"
        private const val KEY_BLOCKED_URLS = "blocked_urls"
        private const val KEY_COMPANION_URL = "companion_url"
        private const val KEY_COMPANION_TOKEN = "companion_token"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_SYNC_ERROR = "last_sync_error"

        const val DEFAULT_EXIT_DELAY_HOURS = 24
        const val MAX_EMERGENCY_PER_MONTH = 2
        const val EMERGENCY_HOLD_SECONDS = 60
    }
}

object DefaultAllowList {
    val PACKAGES: Set<String> = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.google.android.apps.maps",
        "com.google.android.calendar",
        "com.android.camera2",
        "com.google.android.GoogleCamera",
        "com.android.chrome",
        "com.microsoft.emmx",
        "org.mozilla.firefox",
        "com.brave.browser",
    )
}

object DefaultBlockedUrls {
    val DOMAINS: Set<String> = setOf(
        "instagram.com",
        "youtube.com",
        "youtu.be",
        "m.youtube.com",
        "tiktok.com",
        "reddit.com",
        "facebook.com",
        "x.com",
        "twitter.com",
        "snapchat.com",
        "pinterest.com",
        "threads.net",
    )
}
