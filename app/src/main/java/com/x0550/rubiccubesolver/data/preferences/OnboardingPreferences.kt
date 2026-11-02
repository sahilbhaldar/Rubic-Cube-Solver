package com.x0550.rubiccubesolver.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val hasSeenScanInstructions: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SCAN_INSTRUCTIONS_SEEN] ?: false
    }

    suspend fun markScanInstructionsSeen() {
        dataStore.edit { preferences ->
            preferences[SCAN_INSTRUCTIONS_SEEN] = true
        }
    }

    private companion object {
        val SCAN_INSTRUCTIONS_SEEN = booleanPreferencesKey("scan_instructions_seen")
    }
}