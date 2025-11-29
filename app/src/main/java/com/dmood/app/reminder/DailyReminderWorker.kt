package com.dmood.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dmood.app.di.DmoodServiceLocator
import java.time.LocalDate
import java.time.ZoneId

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val decisionRepository = DmoodServiceLocator.decisionRepository
    private val userPreferencesRepository = DmoodServiceLocator.userPreferencesRepository
    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun doWork(): Result {
        if (!userPreferencesRepository.isDailyReminderEnabled()) {
            return Result.success()
        }

        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        return runCatching {
            val decisions = decisionRepository.getByRange(start, end)
            if (decisions.isEmpty()) {
                ReminderNotificationHelper.showDailyReminder(applicationContext)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
