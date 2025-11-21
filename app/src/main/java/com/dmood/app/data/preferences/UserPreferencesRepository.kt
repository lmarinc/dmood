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
import java.time.LocalDate
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
        private val FIRST_USE_DATE_KEY = longPreferencesKey("first_use_date_epoch_day")
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val onboardingStatusFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    val cardLayoutFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[CARD_LAYOUT_KEY]
    }

    val weekStartDayFlow: Flow<DayOfWeek> = dataStore.data.map { prefs ->
        prefs[WEEK_START_DAY_KEY]?.let { DayOfWeek.valueOf(it) } ?: DayOfWeek.MONDAY
    }

    val firstUseDateFlow: Flow<LocalDate?> = dataStore.data.map { prefs ->
        prefs[FIRST_USE_DATE_KEY]?.let { epochDay -> LocalDate.ofEpochDay(epochDay) }
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

    suspend fun saveCardLayoutMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[CARD_LAYOUT_KEY] = mode
        }
    }

    suspend fun setWeekStartDay(day: DayOfWeek) {
        dataStore.edit { prefs ->
            prefs[WEEK_START_DAY_KEY] = day.name
        }
    }

    suspend fun ensureFirstUseDate(today: LocalDate): LocalDate {
        var storedDate: LocalDate? = null
        dataStore.edit { prefs ->
            val current = prefs[FIRST_USE_DATE_KEY]?.let { LocalDate.ofEpochDay(it) }
            val normalized = current ?: today
            prefs[FIRST_USE_DATE_KEY] = normalized.toEpochDay()
            storedDate = normalized
        }
        return storedDate ?: today
    }
}
