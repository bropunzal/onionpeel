package com.ateeb.onionpeel.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserManager
import android.util.Log
import com.ateeb.onionpeel.admin.OnionpeelDeviceAdminReceiver
import com.ateeb.onionpeel.data.PrefsRepository

class PeelModeController(
    private val context: Context,
    private val prefs: PrefsRepository,
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin: ComponentName =
        ComponentName(context, OnionpeelDeviceAdminReceiver::class.java)

    private val urlPolicy = BrowserUrlPolicy(context, admin, dpm)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    fun isAdminActive(): Boolean = dpm.isAdminActive(admin)

    fun enablePeelMode(): Result<Unit> {
        if (!isDeviceOwner()) {
            return Result.failure(IllegalStateException("Not device owner"))
        }
        return runCatching {
            applyInstallRestrictions(enable = true)
            suspendNonAllowedPackages()
            urlPolicy.apply(prefs.getBlockedUrls())
            prefs.peelModeActive = true
            prefs.clearExitRequest()
            Log.i(TAG, "Peel mode enabled")
        }
    }

    fun disablePeelMode(): Result<Unit> {
        if (!isDeviceOwner()) {
            return Result.failure(IllegalStateException("Not device owner"))
        }
        return runCatching {
            unsuspendAllUserPackages()
            applyInstallRestrictions(enable = false)
            urlPolicy.clear()
            prefs.peelModeActive = false
            prefs.clearExitRequest()
            Log.i(TAG, "Peel mode disabled")
        }
    }

    fun applyUrlBlocklist() {
        if (!isDeviceOwner() || !prefs.peelModeActive) return
        urlPolicy.apply(prefs.getBlockedUrls())
    }

    fun requestExit(): Result<Long> {
        if (!prefs.peelModeActive) {
            return Result.failure(IllegalStateException("Peel mode is not active"))
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
        disablePeelMode()
        return true
    }

    fun useEmergencyPass(): Result<Unit> {
        resetEmergencyCounterIfNewMonth()
        if (prefs.emergencyUsesThisMonth >= PrefsRepository.MAX_EMERGENCY_PER_MONTH) {
            return Result.failure(IllegalStateException("No emergency passes left this month"))
        }
        prefs.emergencyUsesThisMonth += 1
        disablePeelMode()
        return Result.success(Unit)
    }

    fun reapplyIfActive() {
        if (!isDeviceOwner() || !prefs.peelModeActive) return
        tryCompleteExitIfReady()
        if (!prefs.peelModeActive) return
        applyInstallRestrictions(enable = true)
        suspendNonAllowedPackages()
        urlPolicy.apply(prefs.getBlockedUrls())
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
        val toSuspend = mutableSetOf<String>()

        // Preinstalled feed apps (e.g. YouTube on Samsung) are system packages and
        // are excluded from installedUserPackages() — suspend them explicitly.
        for (pkg in HARD_BLOCK_APPS) {
            if (isInstalled(pkg)) toSuspend.add(pkg)
        }

        for (app in installedUserPackages()) {
            val pkg = app.packageName
            if (pkg in HARD_BLOCK_APPS) continue
            if (pkg in allow) continue
            if (isEssentialSystem(pkg)) continue
            toSuspend.add(pkg)
        }

        suspendInChunks(toSuspend.toList(), suspend = true)
        Log.i(TAG, "Suspended ${toSuspend.size} packages")
    }

    private fun unsuspendAllUserPackages() {
        val suspended = context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
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

    private fun isInstalled(packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    private fun isEssentialSystem(packageName: String): Boolean {
        val essentials = setOf(
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.systemui",
            "com.android.providers.telephony",
            "com.google.android.gms",
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
        private const val TAG = "PeelModeController"

        /** Feed apps — suspended in peel mode regardless of allow-list mistakes */
        val HARD_BLOCK_APPS: Set<String> = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.facebook.katana",
            "com.snapchat.android",
        )
    }
}
