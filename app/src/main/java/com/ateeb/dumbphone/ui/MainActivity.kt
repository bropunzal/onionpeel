package com.ateeb.dumbphone.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ateeb.dumbphone.data.PrefsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val state by viewModel.uiState.collectAsState()
                Scaffold { padding ->
                    MainScreen(
                        state = state,
                        modifier = Modifier.padding(padding),
                        onRefresh = viewModel::refresh,
                        onToggleApp = viewModel::toggleApp,
                        onSetExitDelay = viewModel::setExitDelayHours,
                        onSetPhrase = viewModel::setEmergencyPhrase,
                        onCompleteSetup = viewModel::completeSetup,
                        onEnable = viewModel::enableDumbMode,
                        onRequestExit = viewModel::requestExit,
                        onCancelExit = viewModel::cancelExit,
                        onConfirmExit = viewModel::confirmExitIfReady,
                        onStartHold = viewModel::startEmergencyHold,
                        onEmergency = viewModel::completeEmergency,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onToggleApp: (String) -> Unit,
    onSetExitDelay: (Int) -> Unit,
    onSetPhrase: (String) -> Unit,
    onCompleteSetup: () -> Unit,
    onEnable: () -> Unit,
    onRequestExit: () -> Unit,
    onCancelExit: () -> Unit,
    onConfirmExit: () -> Unit,
    onStartHold: () -> Unit,
    onEmergency: (String) -> Unit,
) {
    var phraseDraft by remember(state.emergencyPhrase) { mutableStateOf(state.emergencyPhrase) }
    var emergencyTyped by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Dumbphone", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Device Owner allow-list. Only checked apps run. Everything else is suspended at the OS.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (!state.isDeviceOwner) {
            item { SetupCard() }
            return@LazyColumn
        }

        item {
            StatusCard(state)
        }

        state.message?.let { msg ->
            item { Text(msg, color = MaterialTheme.colorScheme.error) }
        }

        if (!state.setupCompleted) {
            item {
                SetupWizardCard(
                    exitDelayHours = state.exitDelayHours,
                    phrase = phraseDraft,
                    onPhraseChange = {
                        phraseDraft = it
                        onSetPhrase(it)
                    },
                    onExitDelayChange = onSetExitDelay,
                    onComplete = onCompleteSetup,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.dumbModeActive) {
                    Button(onClick = onEnable, enabled = state.setupCompleted) {
                        Text("Enable dumb mode")
                    }
                } else if (state.exitReadyAt <= 0L) {
                    Button(onClick = onRequestExit) {
                        Text("Request exit (${state.exitDelayHours}h delay)")
                    }
                } else {
                    val ready = System.currentTimeMillis() >= state.exitReadyAt
                    if (ready) {
                        Button(onClick = onConfirmExit) { Text("Exit dumb mode now") }
                    } else {
                        Button(onClick = onCancelExit) { Text("Cancel exit request") }
                    }
                }
                Button(onClick = onRefresh) { Text("Refresh") }
            }
        }

        if (state.dumbModeActive && state.exitReadyAt > 0L) {
            item { ExitCountdownCard(state.exitReadyAt) }
        }

        item {
            Text("Allowed apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Uncheck feeds and browsers. This app and your launcher are always kept.")
        }

        items(state.apps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = app.allowed, onCheckedChange = { onToggleApp(app.packageName) })
                Column {
                    Text(app.label)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (state.dumbModeActive) {
            item {
                EmergencyCard(
                    usesLeft = state.emergencyUsesLeft,
                    holdProgress = state.holdProgress,
                    holdActive = state.holdActive,
                    holdCompleted = state.holdCompleted,
                    typed = emergencyTyped,
                    onTypedChange = { emergencyTyped = it },
                    onStartHold = onStartHold,
                    onSubmit = { onEmergency(emergencyTyped) },
                )
            }
        }
    }
}

@Composable
private fun SetupCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Provision as Device Owner", fontWeight = FontWeight.SemiBold)
            Text("1. Factory reset the phone. Do not add a Google account.")
            Text("2. Enable USB debugging. Install this APK via ADB.")
            Text("3. Run:")
            Text(
                "adb shell dpm set-device-owner com.ateeb.dumbphone/.admin.DumbphoneDeviceAdminReceiver",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("See README.md in the project for full steps.")
        }
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (state.dumbModeActive) "DUMB MODE ON" else "Dumb mode off", fontWeight = FontWeight.Bold)
            Text("Device owner: yes")
            Text("Exit delay: ${state.exitDelayHours} hours")
            Text("Emergency passes left: ${state.emergencyUsesLeft}")
        }
    }
}

@Composable
private fun SetupWizardCard(
    exitDelayHours: Int,
    phrase: String,
    onPhraseChange: (String) -> Unit,
    onExitDelayChange: (Int) -> Unit,
    onComplete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("First-time setup", fontWeight = FontWeight.SemiBold)
            Text("Exit delay: $exitDelayHours hours")
            Slider(
                value = exitDelayHours.toFloat(),
                onValueChange = { onExitDelayChange(it.toInt()) },
                valueRange = 1f..72f,
                steps = 71,
            )
            OutlinedTextField(
                value = phrase,
                onValueChange = onPhraseChange,
                label = { Text("Emergency phrase (required)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onComplete) { Text("Save setup") }
        }
    }
}

@Composable
private fun ExitCountdownCard(readyAtMillis: Long) {
    val remaining = (readyAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val hours = TimeUnit.MILLISECONDS.toHours(remaining)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Exit countdown", fontWeight = FontWeight.SemiBold)
            Text("Time left: ${hours}h ${minutes}m")
            Text("Ready at: ${fmt.format(Date(readyAtMillis))}")
        }
    }
}

@Composable
private fun EmergencyCard(
    usesLeft: Int,
    holdProgress: Float,
    holdActive: Boolean,
    holdCompleted: Boolean,
    typed: String,
    onTypedChange: (String) -> Unit,
    onStartHold: () -> Unit,
    onSubmit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Emergency exit ($usesLeft left this month)", fontWeight = FontWeight.SemiBold)
            Text("Hold the button ${PrefsRepository.EMERGENCY_HOLD_SECONDS}s, then type your phrase.")
            if (holdActive) {
                LinearProgressIndicator(progress = { holdProgress }, modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = onStartHold, enabled = !holdActive && usesLeft > 0) {
                Text(if (holdActive) "Hold…" else "Start hold")
            }
            OutlinedTextField(
                value = typed,
                onValueChange = onTypedChange,
                label = { Text("Emergency phrase") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSubmit, enabled = !holdActive && holdCompleted) {
                Text("Use emergency pass")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
