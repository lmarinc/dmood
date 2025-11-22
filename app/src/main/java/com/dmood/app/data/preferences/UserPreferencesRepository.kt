package com.dmood.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "dmood_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.userPreferencesDataStore

    companion object {
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val CARD_LAYOUT_KEY = stringPreferencesKey("card_layout_mode")
        private val WEEK_START_DAY_KEY = stringPreferencesKey("week_start_day")
        private val FIRST_USE_DATE_KEY = longPreferencesKey("first_use_date")
        private val DEVELOPER_MODE_KEY = booleanPreferencesKey("developer_mode")
        private const val DEFAULT_WEEK_START = "MONDAY"
        private const val DEFAULT_CARD_LAYOUT = "COZY"
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val onboardingStatusFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    val cardLayoutFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[CARD_LAYOUT_KEY] ?: DEFAULT_CARD_LAYOUT
    }

    val weekStartDayFlow: Flow<DayOfWeek> = dataStore.data.map { prefs ->
        val stored = prefs[WEEK_START_DAY_KEY]
        stored?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
            ?: DayOfWeek.valueOf(DEFAULT_WEEK_START)
    }

    val developerModeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEVELOPER_MODE_KEY] ?: false
    }

    val firstUseDateFlow: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[FIRST_USE_DATE_KEY]
    }

    suspend fun hasSeenOnboarding(): Boolean = onboardingStatusFlow.first()

    suspend fun setHasSeenOnboarding(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[HAS_SEEN_ONBOARDING_KEY] = value
        }
    }

    suspend fun getUserName(): String? = userNameFlow.first()

    suspend fun setUserName(name: String) {
        dataStore.edit { prefs: MutablePreferences ->
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun setCardLayout(mode: String) {
        dataStore.edit { prefs ->
            prefs[CARD_LAYOUT_KEY] = mode
        }
    }

    suspend fun setWeekStartDay(dayOfWeek: DayOfWeek) {
        dataStore.edit { prefs ->
            prefs[WEEK_START_DAY_KEY] = dayOfWeek.name
        }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DEVELOPER_MODE_KEY] = enabled
        }
    }

    suspend fun ensureFirstUseDate(now: Long = System.currentTimeMillis()): Long {
        var stored = firstUseDateFlow.first()
        if (stored == null) {
            dataStore.edit { prefs ->
                prefs[FIRST_USE_DATE_KEY] = now
            }
            stored = now
        }
        return stored
    }

    suspend fun updateFirstUseDateIfEarlier(candidateDate: Long) {
        dataStore.edit { prefs ->
            val current = prefs[FIRST_USE_DATE_KEY]
            val earliest = current?.let { minOf(it, candidateDate) } ?: candidateDate
            prefs[FIRST_USE_DATE_KEY] = earliest
        }
    }

    fun firstUseLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): Flow<LocalDate?> {
        return firstUseDateFlow.map { millis ->
            millis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        }
    }
}
