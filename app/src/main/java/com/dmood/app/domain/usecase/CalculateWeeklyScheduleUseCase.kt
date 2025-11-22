package com.dmood.app.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class WeeklySchedule(
    val anchorDate: LocalDate,
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val nextSummaryDate: LocalDate,
    val eligibleAnchor: LocalDate,
    val isSummaryAvailable: Boolean
)

class CalculateWeeklyScheduleUseCase {

    operator fun invoke(
        firstUseDate: LocalDate,
        weekStartDay: DayOfWeek,
        today: LocalDate = LocalDate.now()
    ): WeeklySchedule {
        val eligibleAnchor = firstEligibleAnchor(firstUseDate, weekStartDay)
        val anchorDate = today.with(TemporalAdjusters.previousOrSame(weekStartDay))
        val windowStart = anchorDate.minusDays(7)
        val windowEnd = anchorDate.minusDays(1)

        val isSummaryAvailable = !today.isBefore(eligibleAnchor) && !anchorDate.isBefore(eligibleAnchor)
        val nextSummaryDate = if (today.isBefore(eligibleAnchor)) {
            eligibleAnchor
        } else {
            anchorDate.plusWeeks(1)
        }

        return WeeklySchedule(
            anchorDate = anchorDate,
            windowStart = windowStart,
            windowEnd = windowEnd,
            nextSummaryDate = nextSummaryDate,
            eligibleAnchor = eligibleAnchor,
            isSummaryAvailable = isSummaryAvailable
        )
    }

    private fun firstEligibleAnchor(firstUseDate: LocalDate, weekStartDay: DayOfWeek): LocalDate {
        var candidate = firstUseDate.with(TemporalAdjusters.nextOrSame(weekStartDay))
        val daysUntilCandidate = ChronoUnit.DAYS.between(firstUseDate, candidate)
        if (daysUntilCandidate < 3) {
            candidate = candidate.plusWeeks(1)
        }
        return candidate
    }
}

