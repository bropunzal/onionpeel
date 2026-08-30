package com.ateeb.onionpeel.companion

import android.util.Log
import com.ateeb.onionpeel.data.PrefsRepository
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CompanionClient(private val prefs: PrefsRepository) {

    fun syncPeelDesired(): Result<Boolean> {
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
                json.getBoolean("peelDesired")
            } finally {
                conn.disconnect()
            }
        }.onFailure { e ->
            prefs.lastCompanionError = e.message ?: "sync failed"
            Log.w(TAG, "Companion sync failed: ${e.message}")
        }
    }

    fun reportPeelState(active: Boolean) {
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
                val payload = JSONObject().put("peelActive", active).toString()
                conn.outputStream.use { it.write(payload.toByteArray()) }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
    }

    companion object {
        private const val TAG = "CompanionClient"
    }
}
