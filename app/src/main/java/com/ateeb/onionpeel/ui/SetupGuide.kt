package com.ateeb.onionpeel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ateeb.onionpeel.ui.theme.OnionCoral
import com.ateeb.onionpeel.ui.theme.OnionCream
import com.ateeb.onionpeel.ui.theme.OnionCreamMuted
import com.ateeb.onionpeel.ui.theme.OnionStroke
import com.ateeb.onionpeel.ui.theme.OnionSurface
import com.ateeb.onionpeel.ui.theme.OnionType

private data class SetupStep(
    val number: Int,
    val title: String,
    val body: String,
    val highlight: Boolean = false,
)

private val provisioningSteps = listOf(
    SetupStep(
        1,
        "Back up your phone",
        "Photos, contacts, anything you need. This process erases the device.",
    ),
    SetupStep(
        2,
        "Factory reset",
        "Settings → General management → Reset → Factory data reset. Confirm and wait for the phone to reboot.",
    ),
    SetupStep(
        3,
        "Skip Google account",
        "During setup, tap Skip or Set up offline. Do not sign in to Google yet — accounts block Device Owner.",
        highlight = true,
    ),
    SetupStep(
        4,
        "Skip Samsung / manufacturer account",
        "Samsung: skip Sign in to Samsung account. Other brands: skip any OEM account prompts.",
        highlight = true,
    ),
    SetupStep(
        5,
        "Connect Wi‑Fi",
        "Any network with internet. Cloud sync works over HTTPS — no PC on the same Wi‑Fi needed.",
    ),
    SetupStep(
        6,
        "Enable USB debugging",
        "Settings → About phone → tap Build number 7 times → Developer options → USB debugging ON.",
    ),
    SetupStep(
        7,
        "Install app",
        "From Google Play closed testing, or on a PC:\nadb install app.apk",
    ),
    SetupStep(
        8,
        "Set Device Owner",
        "Immediately after install, before adding any account:\nadb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver",
        highlight = true,
    ),
)

private val companionSteps = listOf(
    SetupStep(
        9,
        "Open cloud companion",
        "Go to the OnionPeel companion URL in any browser. Click Create new device.",
    ),
    SetupStep(
        10,
        "Copy URL + token",
        "Copy Server URL (https://…) and pairing token from the companion page.",
    ),
    SetupStep(
        11,
        "Link companion (this app)",
        "Enter Server URL + token below → LINK DESKTOP. Wait ~15 seconds for first sync.",
        highlight = true,
    ),
    SetupStep(
        12,
        "Configure in browser",
        "Blocked sites, allowed apps, and unpeel delay hours are edited only in the companion browser.",
    ),
    SetupStep(
        13,
        "Peel from browser",
        "Hit Peel phone in the companion. The phone cannot peel or unpeel itself.",
        highlight = true,
    ),
)

private val afterSetupNotes = listOf(
    "You may add a Google account after Device Owner is set (Gmail, Play Store, backup).",
    "Do not add accounts before step 8 or set-device-owner will fail.",
    "App updates require the phone to be OPEN — request unpeel from the companion first.",
    "Save your pairing token — you need it to control peel from any browser.",
)

@Composable
fun SetupGuideCard(
    isDeviceOwner: Boolean,
    companionPaired: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable {
        mutableStateOf(!isDeviceOwner || !companionPaired)
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(OnionSurface)
            .border(1.dp, OnionStroke, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text("SETUP GUIDE", style = OnionType.section, color = OnionCreamMuted)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                !isDeviceOwner -> "Alpha setup — factory reset required before Device Owner."
                !companionPaired -> "Device Owner OK. Link the cloud companion next."
                else -> "Reference — provisioning and daily control."
            },
            style = OnionType.body,
            color = OnionCreamMuted,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                if (expanded) "Hide steps" else "Show steps",
                color = OnionCreamMuted,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            val steps = when {
                !isDeviceOwner -> provisioningSteps
                !companionPaired -> companionSteps
                else -> provisioningSteps + companionSteps
            }
            steps.forEach { step ->
                SetupStepRow(step)
                Spacer(Modifier.height(12.dp))
            }
            Text("NOTES", style = OnionType.section, color = OnionCreamMuted)
            Spacer(Modifier.height(8.dp))
            afterSetupNotes.forEach { note ->
                Text("· $note", style = OnionType.body, color = OnionCreamMuted)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SetupStepRow(step: SetupStep) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "${step.number}. ${step.title.uppercase()}",
            style = OnionType.section,
            color = if (step.highlight) OnionCoral else OnionCreamMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            step.body,
            style = OnionType.body,
            color = if (step.highlight) OnionCream else OnionCreamMuted,
        )
    }
}
