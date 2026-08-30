package com.ateeb.onionpeel.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ateeb.onionpeel.admin.OnionpeelDeviceAdminReceiver

/**
 * Pushes URL blocklists into browsers that support Chrome Enterprise managed
 * configuration (Chrome, Edge). Browsers stay installed and launchable; only
 * listed sites are blocked inside them.
 */
class BrowserUrlPolicy(
    private val context: Context,
    private val admin: ComponentName,
    private val dpm: DevicePolicyManager,
) {
    fun apply(blocklist: Collection<String>) {
        val normalized = blocklist
            .map { normalizeHost(it) }
            .filter { it.isNotEmpty() }
            .distinct()
            .toTypedArray()

        val restrictions = Bundle().apply {
            putStringArray(KEY_URL_BLOCKLIST, normalized)
            putInt(KEY_INCOGNITO, INCOGNITO_DISABLED)
        }

        for (browser in MANAGED_BROWSERS) {
            if (!isInstalled(browser)) continue
            runCatching {
                dpm.setApplicationRestrictions(admin, browser, restrictions)
                Log.i(TAG, "URL policy applied to $browser (${normalized.size} hosts)")
            }.onFailure { e ->
                Log.w(TAG, "Failed URL policy for $browser: ${e.message}")
            }
        }
    }

    fun clear() {
        for (browser in MANAGED_BROWSERS) {
            if (!isInstalled(browser)) continue
            runCatching {
                dpm.setApplicationRestrictions(admin, browser, Bundle())
            }.onFailure { e ->
                Log.w(TAG, "Failed clearing URL policy for $browser: ${e.message}")
            }
        }
    }

    private fun isInstalled(packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    private fun normalizeHost(raw: String): String {
        var host = raw.trim().lowercase()
        host = host.removePrefix("https://").removePrefix("http://")
        host = host.removePrefix("www.")
        val slash = host.indexOf('/')
        if (slash >= 0) host = host.substring(0, slash)
        return host
    }

    companion object {
        private const val TAG = "BrowserUrlPolicy"
        private const val KEY_URL_BLOCKLIST = "URLBlocklist"
        private const val KEY_INCOGNITO = "IncognitoModeAvailability"
        private const val INCOGNITO_DISABLED = 1

        val MANAGED_BROWSERS: List<String> = listOf(
            "com.android.chrome",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
        )
    }
}
