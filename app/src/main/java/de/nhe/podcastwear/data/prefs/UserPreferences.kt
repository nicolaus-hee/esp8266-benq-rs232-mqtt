package de.nhe.podcastwear.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("gpodder_server_url")
        private val KEY_USERNAME = stringPreferencesKey("gpodder_username")
        private val KEY_PASSWORD = stringPreferencesKey("gpodder_password")
        private val KEY_DEVICE_ID = stringPreferencesKey("gpodder_device_id")
        private val KEY_LAST_SYNC_TS = longPreferencesKey("last_sync_timestamp")
        private val KEY_LAST_EPISODE_SYNC_TS = longPreferencesKey("last_episode_sync_ts")
        private val KEY_SYNC_INTERVAL_HOURS = longPreferencesKey("sync_interval_hours")
        private val KEY_WIFI_ONLY_DOWNLOADS = stringPreferencesKey("wifi_only_downloads")
    }

    data class GpodderConfig(
        val serverUrl: String,
        val username: String,
        val password: String,
        val deviceId: String,
    ) {
        val isConfigured get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    val gpodderConfig: Flow<GpodderConfig> = context.dataStore.data.map { prefs ->
        GpodderConfig(
            serverUrl = prefs[KEY_SERVER_URL] ?: "",
            username = prefs[KEY_USERNAME] ?: "",
            password = prefs[KEY_PASSWORD] ?: "",
            deviceId = prefs[KEY_DEVICE_ID] ?: "podcastwear",
        )
    }

    val lastSyncTimestamp: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNC_TS] ?: 0L }
    val lastEpisodeSyncTimestamp: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_EPISODE_SYNC_TS] ?: 0L }
    val syncIntervalHours: Flow<Long> = context.dataStore.data.map { it[KEY_SYNC_INTERVAL_HOURS] ?: 4L }
    val wifiOnlyDownloads: Flow<Boolean> = context.dataStore.data.map { it[KEY_WIFI_ONLY_DOWNLOADS] != "false" }

    suspend fun saveGpodderConfig(serverUrl: String, username: String, password: String, deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl.trimEnd('/')
            prefs[KEY_USERNAME] = username
            prefs[KEY_PASSWORD] = password
            prefs[KEY_DEVICE_ID] = deviceId
        }
    }

    suspend fun updateLastSyncTimestamp(ts: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC_TS] = ts }
    }

    suspend fun updateLastEpisodeSyncTimestamp(ts: Long) {
        context.dataStore.edit { it[KEY_LAST_EPISODE_SYNC_TS] = ts }
    }

    suspend fun setSyncIntervalHours(hours: Long) {
        context.dataStore.edit { it[KEY_SYNC_INTERVAL_HOURS] = hours }
    }

    suspend fun setWifiOnlyDownloads(wifiOnly: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY_DOWNLOADS] = if (wifiOnly) "true" else "false" }
    }
}
