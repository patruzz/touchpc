package com.touchpc.remotecontrol.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class TouchSettings(
    val sensitivity: Float = 1.0f,
    val acceleration: Float = 1.0f,
    val tapToClick: Boolean = true,
    val scrollSpeed: Float = 1.0f,
    val naturalScrolling: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val themeMode: Int = 0
)

data class ServerHistoryEntry(
    val name: String,
    val host: String,
    val port: Int,
    val lastConnected: Long
)

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_SENSITIVITY = floatPreferencesKey("sensitivity")
        private val KEY_ACCELERATION = floatPreferencesKey("acceleration")
        private val KEY_TAP_TO_CLICK = booleanPreferencesKey("tap_to_click")
        private val KEY_SCROLL_SPEED = floatPreferencesKey("scroll_speed")
        private val KEY_NATURAL_SCROLL = booleanPreferencesKey("natural_scrolling")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration")
        private val KEY_THEME = intPreferencesKey("theme_mode")
        private val KEY_SERVER_HISTORY = stringPreferencesKey("server_history")
    }

    val settings: Flow<TouchSettings> = dataStore.data.map { prefs ->
        TouchSettings(
            sensitivity = prefs[KEY_SENSITIVITY] ?: 1.0f,
            acceleration = prefs[KEY_ACCELERATION] ?: 1.0f,
            tapToClick = prefs[KEY_TAP_TO_CLICK] ?: true,
            scrollSpeed = prefs[KEY_SCROLL_SPEED] ?: 1.0f,
            naturalScrolling = prefs[KEY_NATURAL_SCROLL] ?: false,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            themeMode = prefs[KEY_THEME] ?: 0
        )
    }

    val serverHistory: Flow<List<ServerHistoryEntry>> = dataStore.data.map { prefs ->
        val json = prefs[KEY_SERVER_HISTORY] ?: "[]"
        parseServerHistory(json)
    }

    suspend fun updateSensitivity(value: Float) {
        dataStore.edit { it[KEY_SENSITIVITY] = value }
    }

    suspend fun updateAcceleration(value: Float) {
        dataStore.edit { it[KEY_ACCELERATION] = value }
    }

    suspend fun updateTapToClick(enabled: Boolean) {
        dataStore.edit { it[KEY_TAP_TO_CLICK] = enabled }
    }

    suspend fun updateScrollSpeed(value: Float) {
        dataStore.edit { it[KEY_SCROLL_SPEED] = value }
    }

    suspend fun updateNaturalScrolling(enabled: Boolean) {
        dataStore.edit { it[KEY_NATURAL_SCROLL] = enabled }
    }

    suspend fun updateVibration(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun updateThemeMode(mode: Int) {
        dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun addServerToHistory(name: String, host: String, port: Int) {
        dataStore.edit { prefs ->
            val json = prefs[KEY_SERVER_HISTORY] ?: "[]"
            val list = parseServerHistory(json).toMutableList()
            list.removeAll { it.host == host && it.port == port }
            list.add(0, ServerHistoryEntry(name, host, port, System.currentTimeMillis()))
            if (list.size > 10) list.subList(10, list.size).clear()
            prefs[KEY_SERVER_HISTORY] = serializeServerHistory(list)
        }
    }

    suspend fun clearServerHistory() {
        dataStore.edit { it[KEY_SERVER_HISTORY] = "[]" }
    }

    private fun parseServerHistory(json: String): List<ServerHistoryEntry> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ServerHistoryEntry(
                    name = obj.optString("name", ""),
                    host = obj.getString("host"),
                    port = obj.getInt("port"),
                    lastConnected = obj.optLong("lastConnected", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeServerHistory(list: List<ServerHistoryEntry>): String {
        val array = JSONArray()
        list.forEach { entry ->
            array.put(JSONObject().apply {
                put("name", entry.name)
                put("host", entry.host)
                put("port", entry.port)
                put("lastConnected", entry.lastConnected)
            })
        }
        return array.toString()
    }
}
