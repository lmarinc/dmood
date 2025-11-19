package com.dmood.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "dmood_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.userPreferencesDataStore

    companion object {
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val onboardingStatusFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun hasSeenOnboarding(): Boolean = onboardingStatusFlow.first()

    suspend fun setHasSeenOnboarding(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[HAS_SEEN_ONBOARDING_KEY] = value
        }
    }

    suspend fun getUserName(): String? = userNameFlow.first()

    suspend fun setUserName(name: String) {
        dataStore.edit { prefs: Preferences ->
            prefs[USER_NAME_KEY] = name
        }
    }
}
