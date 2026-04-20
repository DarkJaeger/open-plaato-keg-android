package com.openplaato.keg.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val pourNotificationsKey = booleanPreferencesKey("pour_notifications_enabled")
    // Ordered list of keg IDs — stored as a comma-separated string to preserve insertion order.
    // StringSet prefs have no guaranteed order, so we encode as CSV ourselves.
    private val kegOrderKey = stringPreferencesKey("keg_order")
    private val hasSeenOnboardingKey = booleanPreferencesKey("has_seen_onboarding")

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverUrlKey] ?: ""
    }

    val pourNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pourNotificationsKey] ?: true
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = normalizeServerUrl(url)
        }
    }

    suspend fun setPourNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[pourNotificationsKey] = enabled
        }
    }

    /** Reads the saved keg ordering as an ordered list of IDs. Empty if none saved yet. */
    val kegOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[kegOrderKey]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[hasSeenOnboardingKey] ?: false
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { prefs -> prefs[hasSeenOnboardingKey] = seen }
    }

    /** Persists the full ordered list of keg IDs. */
    suspend fun setKegOrder(orderedIds: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[kegOrderKey] = orderedIds.joinToString(",")
        }
    }

    private fun normalizeServerUrl(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }
}
