package com.dmood.app.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dmood.app.data.preferences.UserPreferencesRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    companion object {
        private const val DAILY_REMINDER_WORK_NAME = "daily_reminder_work"
        private const val WEEKLY_SUMMARY_WORK_NAME = "weekly_summary_work"
    }

    suspend fun syncReminders() {
        if (userPreferencesRepository.isDailyReminderEnabled()) {
            scheduleDailyReminder()
        } else {
            cancelDailyReminder()
        }

        if (userPreferencesRepository.isWeeklyReminderEnabled()) {
            scheduleWeeklySummaryReminder()
        } else {
            cancelWeeklySummaryReminder()
        }
    }

    fun scheduleDailyReminder() {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNext(22, 0), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_REMINDER_WORK_NAME)
    }

    fun scheduleWeeklySummaryReminder() {
        // We run the worker daily to check if a new summary is available.
        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNext(9, 0), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_SUMMARY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelWeeklySummaryReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_SUMMARY_WORK_NAME)
    }

    private fun millisUntilNext(targetHour: Int, targetMinute: Int): Long {
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        var next = now.withHour(targetHour).withMinute(targetMinute)
        if (next.isBefore(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis()
    }
}
