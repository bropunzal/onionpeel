package com.ateeb.dumbphone.policy

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserManager
import android.util.Log
import com.ateeb.dumbphone.admin.DumbphoneDeviceAdminReceiver
import com.ateeb.dumbphone.data.PrefsRepository

class DumbModeController(
    private val context: Context,
    private val prefs: PrefsRepository,
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin: ComponentName =
        ComponentName(context, DumbphoneDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    fun isAdminActive(): Boolean = dpm.isAdminActive(admin)

    fun enableDumbMode(): Result<Unit> {
        if (!isDeviceOwner()) {
            return Result.failure(IllegalStateException("Not device owner"))
        }
        return runCatching {
            applyInstallRestrictions(enable = true)
            suspendNonAllowedPackages()
            prefs.dumbModeActive = true
            prefs.clearExitRequest()
            Log.i(TAG, "Dumb mode enabled")
        }
    }

    fun disableDumbMode(): Result<Unit> {
        if (!isDeviceOwner()) {
            return Result.failure(IllegalStateException("Not device owner"))
        }
        return runCatching {
            unsuspendAllUserPackages()
            applyInstallRestrictions(enable = false)
            prefs.dumbModeActive = false
            prefs.clearExitRequest()
            Log.i(TAG, "Dumb mode disabled")
        }
    }

    fun requestExit(): Result<Long> {
        if (!prefs.dumbModeActive) {
            return Result.failure(IllegalStateException("Dumb mode is not active"))
        }
        if (prefs.exitRequestedAtMillis > 0L) {
            return Result.success(prefs.exitRequestedAtMillis)
        }
        val readyAt = System.currentTimeMillis()
        prefs.exitRequestedAtMillis = readyAt
        return Result.success(readyAt)
    }

    fun cancelExitRequest() {
        prefs.clearExitRequest()
    }

    fun exitReadyAtMillis(): Long {
        val requested = prefs.exitRequestedAtMillis
        if (requested <= 0L) return 0L
        return requested + prefs.exitDelayHours * 60L * 60L * 1000L
    }

    fun tryCompleteExitIfReady(): Boolean {
        val readyAt = exitReadyAtMillis()
        if (readyAt <= 0L || System.currentTimeMillis() < readyAt) {
            return false
        }
        disableDumbMode()
        return true
    }

    fun useEmergencyPass(): Result<Unit> {
        resetEmergencyCounterIfNewMonth()
        if (prefs.emergencyUsesThisMonth >= PrefsRepository.MAX_EMERGENCY_PER_MONTH) {
            return Result.failure(IllegalStateException("No emergency passes left this month"))
        }
        prefs.emergencyUsesThisMonth += 1
        disableDumbMode()
        return Result.success(Unit)
    }

    fun reapplyIfActive() {
        if (!isDeviceOwner() || !prefs.dumbModeActive) return
        tryCompleteExitIfReady()
        if (!prefs.dumbModeActive) return
        applyInstallRestrictions(enable = true)
        suspendNonAllowedPackages()
    }

    private fun applyInstallRestrictions(enable: Boolean) {
        val restrictions = listOf(
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_INSTALL_APPS,
        )
        for (restriction in restrictions) {
            if (enable) {
                dpm.addUserRestriction(admin, restriction)
            } else {
                dpm.clearUserRestriction(admin, restriction)
            }
        }
    }

    private fun suspendNonAllowedPackages() {
        val allow = buildEffectiveAllowList()
        val toSuspend = mutableListOf<String>()

        for (app in installedUserPackages()) {
            val pkg = app.packageName
            if (pkg in DumbModeController.HARD_BLOCK_SUGGESTIONS) {
                toSuspend.add(pkg)
                continue
            }
            if (pkg in allow) continue
            if (isEssentialSystem(pkg)) continue
            toSuspend.add(pkg)
        }

        suspendInChunks(toSuspend, suspend = true)
        Log.i(TAG, "Suspended ${toSuspend.size} packages")
    }

    private fun unsuspendAllUserPackages() {
        val suspended = installedUserPackages()
            .map { it.packageName }
            .filter { pkg ->
                runCatching { dpm.isPackageSuspended(admin, pkg) }.getOrDefault(false)
            }
        suspendInChunks(suspended, suspend = false)
    }

    private fun suspendInChunks(packages: List<String>, suspend: Boolean) {
        packages.chunked(50).forEach { chunk ->
            runCatching {
                dpm.setPackagesSuspended(admin, chunk.toTypedArray(), suspend)
            }.onFailure { e ->
                Log.w(TAG, "setPackagesSuspended failed for chunk: ${e.message}")
            }
        }
    }

    private fun buildEffectiveAllowList(): Set<String> {
        val allow = prefs.getAllowList().toMutableSet()
        allow.add(context.packageName)
        resolveDefaultLauncher()?.let { allow.add(it) }
        return allow
    }

    private fun resolveDefaultLauncher(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return null
        return resolve.activityInfo?.packageName
    }

    private fun installedUserPackages(): List<ApplicationInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || isWhitelistedSystem(it.packageName) }
    }

    private fun isWhitelistedSystem(packageName: String): Boolean {
        return packageName == context.packageName ||
            packageName == resolveDefaultLauncher()
    }

    private fun isEssentialSystem(packageName: String): Boolean {
        // Telephony stack — keep callable even if not on allow-list
        val essentials = setOf(
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.systemui",
            "com.android.providers.telephony",
            "com.google.android.gms", // needed on many devices for dialer/maps
        )
        return packageName in essentials
    }

    private fun resetEmergencyCounterIfNewMonth() {
        val monthKey = java.time.YearMonth.now().toString()
        if (prefs.emergencyMonthKey != monthKey) {
            prefs.emergencyMonthKey = monthKey
            prefs.emergencyUsesThisMonth = 0
        }
    }

    companion object {
        private const val TAG = "DumbModeController"

        /** Known feed / browser packages — always suspended in dumb mode even if user toggles allow-list wrong */
        val HARD_BLOCK_SUGGESTIONS: Set<String> = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.facebook.katana",
            "com.snapchat.android",
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser",
            "com.microsoft.emmx",
        )
    }
}
