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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "dmood_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.userPreferencesDataStore

    companion object {
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val START_OF_WEEK_KEY = stringPreferencesKey("start_of_week")
        private val CARD_LAYOUT_KEY = stringPreferencesKey("card_layout")
        private val FIRST_USAGE_EPOCH_DAY_KEY = longPreferencesKey("first_usage_epoch_day")
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val onboardingStatusFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    val startOfWeekFlow: Flow<DayOfWeek> = dataStore.data.map { prefs ->
        val stored = prefs[START_OF_WEEK_KEY]
        runCatching { DayOfWeek.valueOf(stored ?: DayOfWeek.MONDAY.name) }
            .getOrDefault(DayOfWeek.MONDAY)
    }

    val cardLayoutFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[CARD_LAYOUT_KEY]
    }

    val firstUsageDateFlow: Flow<LocalDate> = dataStore.data.map { prefs ->
        val epochDay = prefs[FIRST_USAGE_EPOCH_DAY_KEY]
        epochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
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

    suspend fun setStartOfWeek(dayOfWeek: DayOfWeek) {
        dataStore.edit { prefs ->
            prefs[START_OF_WEEK_KEY] = dayOfWeek.name
        }
    }

    suspend fun saveCardLayout(layout: String) {
        dataStore.edit { prefs ->
            prefs[CARD_LAYOUT_KEY] = layout
        }
    }

    suspend fun ensureFirstUsageDate(today: LocalDate = LocalDate.now()): LocalDate {
        val stored = dataStore.data.first()[FIRST_USAGE_EPOCH_DAY_KEY]
        if (stored != null) {
            return LocalDate.ofEpochDay(stored)
        }

        dataStore.edit { prefs ->
            prefs[FIRST_USAGE_EPOCH_DAY_KEY] = today.toEpochDay()
        }
        return today
    }

    suspend fun updateFirstUsageDateIfEarlier(candidate: LocalDate) {
        dataStore.edit { prefs ->
            val stored = prefs[FIRST_USAGE_EPOCH_DAY_KEY]
            val storedDate = stored?.let { LocalDate.ofEpochDay(it) }
            val finalDate = when {
                storedDate == null -> candidate
                candidate.isBefore(storedDate) -> candidate
                else -> storedDate
            }
            prefs[FIRST_USAGE_EPOCH_DAY_KEY] = finalDate.toEpochDay()
        }
    }
}
