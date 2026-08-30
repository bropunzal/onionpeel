package com.ateeb.dumbphone.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ateeb.dumbphone.DumbphoneApp
import com.ateeb.dumbphone.data.PrefsRepository
import com.ateeb.dumbphone.policy.DumbModeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstalledApp(
    val packageName: String,
    val label: String,
    val allowed: Boolean,
)

data class MainUiState(
    val isDeviceOwner: Boolean = false,
    val dumbModeActive: Boolean = false,
    val exitRequestedAt: Long = 0L,
    val exitReadyAt: Long = 0L,
    val exitDelayHours: Int = PrefsRepository.DEFAULT_EXIT_DELAY_HOURS,
    val emergencyPhrase: String = "",
    val emergencyUsesLeft: Int = PrefsRepository.MAX_EMERGENCY_PER_MONTH,
    val setupCompleted: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val message: String? = null,
    val holdProgress: Float = 0f,
    val holdActive: Boolean = false,
    val holdCompleted: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = DumbphoneApp.get(application)
    private val prefs = app.prefs
    private val controller = app.dumbMode

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        controller.tryCompleteExitIfReady()
        val readyAt = controller.exitReadyAtMillis()
        _uiState.update {
            it.copy(
                isDeviceOwner = controller.isDeviceOwner(),
                dumbModeActive = prefs.dumbModeActive,
                exitRequestedAt = prefs.exitRequestedAtMillis,
                exitReadyAt = readyAt,
                exitDelayHours = prefs.exitDelayHours,
                emergencyPhrase = prefs.emergencyPhrase,
                emergencyUsesLeft = remainingEmergencies(),
                setupCompleted = prefs.setupCompleted,
                apps = loadLaunchableApps(),
                message = null,
            )
        }
    }

    fun setExitDelayHours(hours: Int) {
        prefs.exitDelayHours = hours
        refresh()
    }

    fun setEmergencyPhrase(phrase: String) {
        prefs.emergencyPhrase = phrase.trim()
        refresh()
    }

    fun completeSetup() {
        if (prefs.emergencyPhrase.length < 4) {
            _uiState.update { it.copy(message = "Set an emergency phrase (4+ characters) first.") }
            return
        }
        prefs.setupCompleted = true
        refresh()
    }

    fun toggleApp(packageName: String) {
        val current = prefs.getAllowList().toMutableSet()
        if (packageName in current) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.setAllowList(current)
        if (prefs.dumbModeActive) {
            controller.reapplyIfActive()
        }
        refresh()
    }

    fun enableDumbMode() {
        if (!prefs.setupCompleted) {
            _uiState.update { it.copy(message = "Finish setup first.") }
            return
        }
        controller.enableDumbMode()
            .onSuccess { refresh() }
            .onFailure { e ->
                _uiState.update { it.copy(message = e.message ?: "Failed to enable") }
            }
    }

    fun requestExit() {
        controller.requestExit()
            .onSuccess { refresh() }
            .onFailure { e ->
                _uiState.update { it.copy(message = e.message ?: "Cannot request exit") }
            }
    }

    fun cancelExit() {
        controller.cancelExitRequest()
        refresh()
    }

    fun confirmExitIfReady() {
        if (controller.tryCompleteExitIfReady()) {
            refresh()
        } else {
            _uiState.update { it.copy(message = "Exit delay not finished yet.") }
        }
    }

    fun startEmergencyHold() {
        if (remainingEmergencies() <= 0) {
            _uiState.update { it.copy(message = "No emergency passes left this month.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(holdActive = true, holdProgress = 0f, holdCompleted = false) }
            val steps = PrefsRepository.EMERGENCY_HOLD_SECONDS
            repeat(steps) { step ->
                kotlinx.coroutines.delay(1000)
                _uiState.update { it.copy(holdProgress = (step + 1) / steps.toFloat()) }
            }
            _uiState.update { it.copy(holdActive = false, holdCompleted = true) }
        }
    }

    fun completeEmergency(typedPhrase: String) {
        if (typedPhrase.trim() != prefs.emergencyPhrase) {
            _uiState.update { it.copy(message = "Phrase does not match.") }
            return
        }
        controller.useEmergencyPass()
            .onSuccess {
                _uiState.update { it.copy(message = "Emergency exit used. Dumb mode is off until you enable it again.") }
                refresh()
            }
            .onFailure { e ->
                _uiState.update { it.copy(message = e.message) }
            }
    }

    private fun remainingEmergencies(): Int {
        val monthKey = java.time.YearMonth.now().toString()
        val used = if (prefs.emergencyMonthKey == monthKey) prefs.emergencyUsesThisMonth else 0
        return (PrefsRepository.MAX_EMERGENCY_PER_MONTH - used).coerceAtLeast(0)
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
                val label = resolve.loadLabel(pm).toString()
                InstalledApp(pkg, label, pkg in allow)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
