package com.dmood.app.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Define cuándo debe liberarse un resumen semanal en función del día de inicio
 * elegido por la persona usuaria y la fecha en la que comenzó a registrar decisiones.
 */
data class WeeklySummarySchedule(
    val latestAvailableDate: LocalDate?,
    val nextReleaseDate: LocalDate,
    val windowStart: LocalDate?,
    val windowEnd: LocalDate?,
    val availableToday: Boolean
)

class CalculateWeeklySummaryScheduleUseCase {

    operator fun invoke(
        today: LocalDate,
        startDay: DayOfWeek,
        firstUseDate: LocalDate,
        minimumTrackedDays: Long = 4
    ): WeeklySummarySchedule {
        val normalizedFirstUse = if (firstUseDate.isAfter(today)) today else firstUseDate

        var firstReleaseCandidate = today.adjustToStartDayFrom(normalizedFirstUse, startDay)
        while (ChronoUnit.DAYS.between(normalizedFirstUse, firstReleaseCandidate) < minimumTrackedDays) {
            firstReleaseCandidate = firstReleaseCandidate.plusWeeks(1)
        }

        if (today.isBefore(firstReleaseCandidate)) {
            return WeeklySummarySchedule(
                latestAvailableDate = null,
                nextReleaseDate = firstReleaseCandidate,
                windowStart = null,
                windowEnd = null,
                availableToday = false
            )
        }

        val weeksSinceFirstRelease = ChronoUnit.WEEKS.between(firstReleaseCandidate, today).toInt()
        val latestReleaseDate = firstReleaseCandidate.plusWeeks(weeksSinceFirstRelease.toLong())
        val windowStart = latestReleaseDate.minusWeeks(1)
        val windowEnd = latestReleaseDate.minusDays(1)

        return WeeklySummarySchedule(
            latestAvailableDate = latestReleaseDate,
            nextReleaseDate = latestReleaseDate.plusWeeks(1),
            windowStart = windowStart,
            windowEnd = windowEnd,
            availableToday = !today.isBefore(latestReleaseDate)
        )
    }

    private fun LocalDate.adjustToStartDayFrom(anchor: LocalDate, startDay: DayOfWeek): LocalDate {
        var candidate = anchor
        while (candidate.dayOfWeek != startDay) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}
