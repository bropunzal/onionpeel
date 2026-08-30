package com.ateeb.onionpeel.companion

import android.util.Log
import com.ateeb.onionpeel.data.PrefsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CompanionClient(private val prefs: PrefsRepository) {

    fun syncFromDesktop(): Result<CompanionSyncPayload> {
        val base = prefs.companionBaseUrl
        val token = prefs.companionToken
        if (base.isBlank() || token.isBlank()) {
            return Result.failure(IllegalStateException("Companion not paired"))
        }

        return runCatching {
            val url = URL("$base/api/sync")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                }
                if (code !in 200..299) error(body)
                val json = JSONObject(body)
                prefs.lastCompanionSyncMillis = System.currentTimeMillis()
                prefs.lastCompanionError = ""
                CompanionSyncPayload(
                    peelDesired = json.getBoolean("peelDesired"),
                    blockedUrls = json.optJSONArray("blockedUrls").toStringSet(),
                    allowList = json.optJSONArray("allowList").toStringSet(),
                    exitDelayHours = json.optInt("exitDelayHours", PrefsRepository.DEFAULT_EXIT_DELAY_HOURS),
                    unpeelAt = json.optLong("unpeelAt").takeIf { json.has("unpeelAt") && !json.isNull("unpeelAt") && it > 0L },
                )
            } finally {
                conn.disconnect()
            }
        }.onFailure { e ->
            prefs.lastCompanionError = e.message ?: "sync failed"
            Log.w(TAG, "Companion sync failed: ${e.message}")
        }
    }

    fun reportPeelState(active: Boolean, apps: List<ReportedApp>) {
        val base = prefs.companionBaseUrl
        val token = prefs.companionToken
        if (base.isBlank() || token.isBlank()) return

        runCatching {
            val url = URL("$base/api/phone/report")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }
            try {
                val appsJson = JSONArray()
                for (app in apps) {
                    appsJson.put(
                        JSONObject()
                            .put("packageName", app.packageName)
                            .put("label", app.label)
                            .put("allowed", app.allowed),
                    )
                }
                val payload = JSONObject()
                    .put("peelActive", active)
                    .put("apps", appsJson)
                    .toString()
                conn.outputStream.use { it.write(payload.toByteArray()) }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            for (i in 0 until length()) {
                val value = optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    companion object {
        private const val TAG = "CompanionClient"
    }
}

data class ReportedApp(
    val packageName: String,
    val label: String,
    val allowed: Boolean,
)
