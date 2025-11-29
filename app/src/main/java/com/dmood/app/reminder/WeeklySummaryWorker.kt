package com.dmood.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dmood.app.di.DmoodServiceLocator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.first

class WeeklySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val userPreferencesRepository = DmoodServiceLocator.userPreferencesRepository
    private val calculateWeeklyScheduleUseCase = DmoodServiceLocator.calculateWeeklyScheduleUseCase
    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun doWork(): Result {
        if (!userPreferencesRepository.isWeeklyReminderEnabled()) {
            return Result.success()
        }

        return runCatching {
            val firstUseMillis = userPreferencesRepository.ensureFirstUseDate()
            val firstUseDate = Instant.ofEpochMilli(firstUseMillis).atZone(zoneId).toLocalDate()
            val weekStartDay = userPreferencesRepository.weekStartDayFlow.first()

            val schedule = calculateWeeklyScheduleUseCase(
                firstUseDate = firstUseDate,
                weekStartDay = weekStartDay,
                today = LocalDate.now(zoneId)
            )

            if (schedule.isSummaryAvailable) {
                val lastAnchor = userPreferencesRepository.getLastWeeklyReminderAnchor()
                if (lastAnchor == null || lastAnchor.isBefore(schedule.anchorDate)) {
                    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    val summaryLabel = "${schedule.windowStart.format(formatter)} - ${schedule.windowEnd.format(formatter)}"
                    ReminderNotificationHelper.showWeeklySummaryReminder(applicationContext, summaryLabel)
                    userPreferencesRepository.setLastWeeklyReminderAnchor(schedule.anchorDate)
                }
            }

            Result.success()
        }.getOrElse { Result.retry() }
    }
}
