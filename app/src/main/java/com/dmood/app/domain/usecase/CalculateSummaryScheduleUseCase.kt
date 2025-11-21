package com.dmood.app.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SummarySchedule(
    val startOfWeek: DayOfWeek,
    val firstEligibleSummary: LocalDate,
    val nextSummaryDate: LocalDate,
    val daysUntilNext: Int,
    val isAvailableToday: Boolean
)

class CalculateSummaryScheduleUseCase {

    operator fun invoke(
        startOfWeek: DayOfWeek,
        firstUsageDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): SummarySchedule {
        val firstEligible = resolveFirstEligibleSummary(startOfWeek, firstUsageDate)
        val nextSummaryDate = resolveNextSummaryDate(firstEligible, startOfWeek, today)
        val daysUntilNext = ChronoUnit.DAYS.between(today, nextSummaryDate).toInt()

        return SummarySchedule(
            startOfWeek = startOfWeek,
            firstEligibleSummary = firstEligible,
            nextSummaryDate = nextSummaryDate,
            daysUntilNext = daysUntilNext,
            isAvailableToday = daysUntilNext == 0
        )
    }

    private fun resolveFirstEligibleSummary(
        startOfWeek: DayOfWeek,
        firstUsageDate: LocalDate
    ): LocalDate {
        val firstCandidate = firstUsageDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(startOfWeek))

        // Necesitamos al menos 4 días registrados antes de liberar el primer resumen.
        var eligible = firstCandidate
        while (ChronoUnit.DAYS.between(firstUsageDate, eligible) < 4) {
            eligible = eligible.plusWeeks(1)
        }
        return eligible
    }

    private fun resolveNextSummaryDate(
        firstEligible: LocalDate,
        startOfWeek: DayOfWeek,
        today: LocalDate
    ): LocalDate {
        if (today.isBefore(firstEligible)) return firstEligible

        val weeksDiff = ChronoUnit.WEEKS.between(firstEligible, today)
        val currentCycleStart = firstEligible.plusWeeks(weeksDiff)

        return if (!today.isBefore(currentCycleStart)) {
            // Si hoy está en el mismo día de inicio de semana, permitimos el resumen en ese día.
            if (today.dayOfWeek == startOfWeek) today else currentCycleStart.plusWeeks(1)
        } else {
            currentCycleStart
        }
    }
}
