package com.ateeb.onionpeel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ateeb.onionpeel.ui.theme.OnionCoral
import com.ateeb.onionpeel.ui.theme.OnionCream
import com.ateeb.onionpeel.ui.theme.OnionCreamMuted
import com.ateeb.onionpeel.ui.theme.OnionInk
import com.ateeb.onionpeel.ui.theme.OnionMint
import com.ateeb.onionpeel.ui.theme.OnionStroke
import com.ateeb.onionpeel.ui.theme.OnionSurface
import com.ateeb.onionpeel.ui.theme.OnionTitanium
import com.ateeb.onionpeel.ui.theme.OnionType
import com.ateeb.onionpeel.ui.theme.OnionpeelTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnionpeelTheme {
                val state by viewModel.uiState.collectAsState()
                Scaffold(
                    containerColor = OnionInk,
                ) { padding ->
                    MainScreen(
                        state = state,
                        modifier = Modifier.padding(padding),
                        onCompleteSetup = viewModel::completeSetup,
                        onSaveCompanion = viewModel::saveCompanion,
                        onToggleApp = viewModel::toggleApp,
                        onAddUrl = viewModel::addBlockedUrl,
                        onRemoveUrl = viewModel::removeBlockedUrl,
                        onRefresh = viewModel::refresh,
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
    onCompleteSetup: () -> Unit,
    onSaveCompanion: (String, String) -> Unit,
    onToggleApp: (String) -> Unit,
    onAddUrl: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var urlDraft by remember(state.companionUrl) { mutableStateOf(state.companionUrl) }
    var tokenDraft by remember(state.companionToken) { mutableStateOf(state.companionToken) }
    var domainDraft by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnionInk),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { BezelLine() }

            item {
                Text("ONIONPEEL", style = OnionType.section, color = OnionCreamMuted)
            }

            if (!state.isDeviceOwner) {
                item {
                    SetupGuideCard(
                        isDeviceOwner = false,
                        companionPaired = false,
                    )
                }
                item { DeviceOwnerCard() }
                return@LazyColumn
            }

            item {
                SetupGuideCard(
                    isDeviceOwner = true,
                    companionPaired = state.companionPaired,
                )
            }

            item { HeroStatus(peeled = state.peelModeActive) }

            if (state.companionPaired) {
                item {
                    StatusMetrics(
                        peeled = state.peelModeActive,
                        syncOk = state.lastSyncError.isEmpty() && state.lastSyncMillis > 0,
                        syncLabel = when {
                            state.lastSyncError.isNotEmpty() -> "error"
                            state.lastSyncMillis > 0 -> "ok"
                            else -> "waiting"
                        },
                    )
                }
            }

            item {
                Text(
                    if (state.peelModeActive) {
                        "Feeds are dead. Chrome URLs blocked. Unpeel from your desktop browser only."
                    } else {
                        "Phone is open. Peel from your desktop companion when you need focus."
                    },
                    style = OnionType.body,
                    color = OnionCreamMuted,
                    textAlign = TextAlign.Start,
                )
            }

            state.message?.let { msg ->
                item { Text(msg, color = OnionCoral) }
            }

            if (!state.companionPaired) {
                item {
                    CompanionPairCard(
                        url = urlDraft,
                        token = tokenDraft,
                        onUrlChange = { urlDraft = it },
                        onTokenChange = { tokenDraft = it },
                        onSave = { onSaveCompanion(urlDraft, tokenDraft) },
                    )
                }
            } else {
                item {
                    SurfaceCard {
                        Text("DESKTOP LINKED", style = OnionType.section, color = OnionCreamMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.companionUrl,
                            style = OnionType.metric,
                            color = OnionCream,
                        )
                        if (state.lastSyncError.isNotEmpty()) {
                            Text("Sync: ${state.lastSyncError}", color = OnionCoral, style = OnionType.body)
                        } else if (state.lastSyncMillis > 0) {
                            Text("Last sync OK", color = OnionMint, style = OnionType.body)
                        }
                    }
                }
            }

            if (!state.setupCompleted && state.companionPaired) {
                item {
                    Text(
                        "Waiting for desktop policy sync…",
                        style = OnionType.body,
                        color = OnionCreamMuted,
                    )
                }
            }

            if (state.companionPaired && state.setupCompleted) {
                item {
                    Text("POLICY", style = OnionType.section, color = OnionCreamMuted)
                    Text(
                        "Blocked sites and allowed apps are managed from your desktop browser.",
                        style = OnionType.body,
                        color = OnionCreamMuted,
                    )
                }
                item {
                    Text("BLOCKED SITES", style = OnionType.section, color = OnionCreamMuted)
                }
                if (state.blockedUrls.isEmpty()) {
                    item { Text("None", color = OnionCreamMuted, style = OnionType.body) }
                } else {
                    items(state.blockedUrls, key = { it }) { url ->
                        Text(url, color = OnionCream, style = OnionType.metric)
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("ALLOWED APPS", style = OnionType.section, color = OnionCreamMuted)
                }
                items(state.apps, key = { it.packageName }) { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = app.allowed,
                            onCheckedChange = null,
                            enabled = false,
                            colors = CheckboxDefaults.colors(checkedColor = OnionCoral),
                        )
                        Column {
                            Text(app.label, color = OnionCream)
                            Text(app.packageName, style = OnionType.body, color = OnionCreamMuted)
                        }
                    }
                }
            } else if (!state.peelModeActive && state.setupCompleted && !state.companionPaired) {
                item {
                    Text("BLOCKED SITES", style = OnionType.section, color = OnionCreamMuted)
                    Text("Chrome, Edge & Samsung Internet · managed URL policy", style = OnionType.body, color = OnionCreamMuted)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = domainDraft,
                            onValueChange = { domainDraft = it },
                            label = { Text("domain.com") },
                            modifier = Modifier.weight(1f),
                            colors = onionFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                        )
                        Button(
                            onClick = { onAddUrl(domainDraft); domainDraft = "" },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OnionTitanium, contentColor = OnionCream),
                        ) { Text("ADD", style = OnionType.section) }
                    }
                }
                items(state.blockedUrls, key = { it }) { url ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(url, color = OnionCream, style = OnionType.metric)
                        TextButton(onClick = { onRemoveUrl(url) }) {
                            Text("Remove", color = OnionCreamMuted)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("ALLOWED APPS", style = OnionType.section, color = OnionCreamMuted)
                }
                items(state.apps, key = { it.packageName }) { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = app.allowed,
                            onCheckedChange = { onToggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(checkedColor = OnionCoral),
                        )
                        Column {
                            Text(app.label, color = OnionCream)
                            Text(app.packageName, style = OnionType.body, color = OnionCreamMuted)
                        }
                    }
                }
            }

            item {
                TextButton(onClick = onRefresh) {
                    Text("Refresh", color = OnionCreamMuted)
                }
            }

            item {
                Text(
                    "v${state.appVersion} · closed beta",
                    style = OnionType.body,
                    color = OnionCreamMuted.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BezelLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(OnionTitanium),
    )
}

@Composable
private fun HeroStatus(peeled: Boolean) {
    Column {
        Text(
            text = "PEEL STATUS",
            style = OnionType.section,
            color = OnionCreamMuted,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (peeled) "PEELED" else "OPEN",
            style = OnionType.hero,
            color = if (peeled) OnionCoral else OnionMint,
        )
    }
}

@Composable
private fun StatusMetrics(
    peeled: Boolean,
    syncOk: Boolean,
    syncLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricTile(label = "PEEL STATE", value = if (peeled) "peeled" else "open", modifier = Modifier.weight(1f))
        MetricTile(
            label = "PHONE SYNC",
            value = syncLabel,
            valueColor = when {
                syncOk -> OnionMint
                syncLabel == "error" -> OnionCoral
                else -> OnionCream
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OnionCream,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OnionInk)
            .border(1.dp, OnionStroke, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(label, style = OnionType.section, color = OnionCreamMuted)
        Spacer(Modifier.height(6.dp))
        Text(value, style = OnionType.metric, color = valueColor)
    }
}

@Composable
private fun SurfaceCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(OnionSurface)
            .border(1.dp, OnionStroke, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun DeviceOwnerCard() {
    SurfaceCard {
        Text("DEVICE OWNER REQUIRED", style = OnionType.section, color = OnionCreamMuted)
        Spacer(Modifier.height(8.dp))
        Text(
            "Complete steps 1–8 in the setup guide above while no Google or Samsung account is on the phone.",
            style = OnionType.body,
            color = OnionCreamMuted,
        )
        Spacer(Modifier.height(8.dp))
        Text("Then run on your PC:", style = OnionType.body, color = OnionCreamMuted)
        Text(
            "adb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver",
            style = OnionType.metric,
            color = OnionCream,
        )
    }
}

@Composable
private fun CompanionPairCard(
    url: String,
    token: String,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    SurfaceCard {
        Text("DESKTOP COMPANION", style = OnionType.section, color = OnionCreamMuted)
        Spacer(Modifier.height(8.dp))
        Text(
            "Run companion on your PC (npm start) or use the cloud URL from CLOUD.md.",
            style = OnionType.body,
            color = OnionCreamMuted,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("https://onionpeel.example.com") },
            modifier = Modifier.fillMaxWidth(),
            colors = onionFieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            label = { Text("Pairing token") },
            modifier = Modifier.fillMaxWidth(),
            colors = onionFieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OnionCoral, contentColor = OnionInk),
        ) {
            Text("LINK DESKTOP", style = OnionType.section)
        }
    }
}

@Composable
private fun onionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OnionCream,
    unfocusedTextColor = OnionCream,
    focusedBorderColor = OnionCoral,
    unfocusedBorderColor = OnionStroke,
    focusedLabelColor = OnionCreamMuted,
    unfocusedLabelColor = OnionCreamMuted,
)

@Preview(showBackground = true, backgroundColor = 0xFF0E0E0C, heightDp = 800)
@Composable
private fun PreviewPeeled() {
    OnionpeelTheme {
        MainScreen(
            state = MainUiState(
                isDeviceOwner = true,
                peelModeActive = true,
                setupCompleted = true,
                companionPaired = true,
                companionUrl = "https://onionpeel.example.com",
            ),
            onCompleteSetup = {}, onSaveCompanion = { _, _ -> },
            onToggleApp = {}, onAddUrl = {}, onRemoveUrl = {}, onRefresh = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0E0C, heightDp = 900)
@Composable
private fun PreviewOpenConfig() {
    OnionpeelTheme {
        MainScreen(
            state = MainUiState(
                isDeviceOwner = true,
                peelModeActive = false,
                setupCompleted = true,
                companionPaired = true,
                blockedUrls = listOf("instagram.com", "youtube.com"),
                apps = previewApps(),
            ),
            onCompleteSetup = {}, onSaveCompanion = { _, _ -> },
            onToggleApp = {}, onAddUrl = {}, onRemoveUrl = {}, onRefresh = {},
        )
    }
}

private fun previewApps() = listOf(
    InstalledApp("com.android.chrome", "Chrome", true),
    InstalledApp("com.google.android.dialer", "Phone", true),
    InstalledApp("com.instagram.android", "Instagram", false),
)
