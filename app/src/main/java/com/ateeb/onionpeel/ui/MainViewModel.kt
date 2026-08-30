package com.ateeb.onionpeel.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import com.ateeb.onionpeel.OnionpeelApp
import com.ateeb.onionpeel.companion.CompanionSyncService
import com.ateeb.onionpeel.data.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InstalledApp(
    val packageName: String,
    val label: String,
    val allowed: Boolean,
)

data class MainUiState(
    val isDeviceOwner: Boolean = false,
    val peelModeActive: Boolean = false,
    val setupCompleted: Boolean = false,
    val companionPaired: Boolean = false,
    val companionUrl: String = "",
    val companionToken: String = "",
    val lastSyncMillis: Long = 0L,
    val lastSyncError: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val blockedUrls: List<String> = emptyList(),
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = OnionpeelApp.get(application)
    private val prefs = app.prefs
    private val controller = app.peelMode

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isDeviceOwner = controller.isDeviceOwner(),
                peelModeActive = prefs.peelModeActive,
                setupCompleted = prefs.setupCompleted,
                companionPaired = prefs.companionPaired,
                companionUrl = prefs.companionBaseUrl,
                companionToken = prefs.companionToken,
                lastSyncMillis = prefs.lastCompanionSyncMillis,
                lastSyncError = prefs.lastCompanionError,
                apps = loadLaunchableApps(),
                blockedUrls = prefs.getBlockedUrls().sorted(),
                message = null,
            )
        }
    }

    fun completeSetup() {
        prefs.setupCompleted = true
        refresh()
    }

    fun saveCompanion(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            _uiState.update { it.copy(message = "Enter desktop URL and token.") }
            return
        }
        prefs.companionBaseUrl = url
        prefs.companionToken = token
        CompanionSyncService.start(getApplication())
        _uiState.update { it.copy(message = "Paired. Peel is controlled from your desktop browser.") }
        refresh()
    }

    fun toggleApp(packageName: String) {
        if (prefs.peelModeActive) return
        val current = prefs.getAllowList().toMutableSet()
        if (packageName in current) current.remove(packageName) else current.add(packageName)
        prefs.setAllowList(current)
        refresh()
    }

    fun addBlockedUrl(url: String) {
        if (prefs.peelModeActive) return
        val normalized = url.trim().lowercase()
        if (normalized.isEmpty()) return
        prefs.addBlockedUrl(normalized)
        refresh()
    }

    fun removeBlockedUrl(url: String) {
        if (prefs.peelModeActive) return
        prefs.removeBlockedUrl(url)
        refresh()
    }

    private fun loadLaunchableApps(): List<InstalledApp> {
        val pm = getApplication<Application>().packageManager
        val allow = prefs.getAllowList()
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == getApplication<Application>().packageName) return@mapNotNull null
                InstalledApp(pkg, resolve.loadLabel(pm).toString(), pkg in allow)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
