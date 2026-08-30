package com.ateeb.onionpeel.companion

data class CompanionSyncPayload(
    val peelDesired: Boolean,
    val blockedUrls: Set<String>,
    val allowList: Set<String>,
    val exitDelayHours: Int,
    val unpeelAt: Long?,
)
