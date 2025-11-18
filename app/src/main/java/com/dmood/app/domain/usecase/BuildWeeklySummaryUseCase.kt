package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class WeeklySummary(
    val startDate: Long,
    val endDate: Long,
    val totalDecisions: Int,
    val calmPercentage: Float,
    val impulsivePercentage: Float,
    val neutralPercentage: Float,
    val dailyMoods: Map<String, DailyMood>,
    val categoryDistribution: Map<CategoryType, Int>
)

class BuildWeeklySummaryUseCase(
    private val calculateDailyMoodUseCase: CalculateDailyMoodUseCase
) {
    operator fun invoke(
        decisions: List<Decision>,
        startDate: Long,
        endDate: Long
    ): WeeklySummary {
        val totalDecisions = decisions.size
        val calmCount = decisions.count { it.tone == DecisionTone.CALMADA }
        val impulsiveCount = decisions.count { it.tone == DecisionTone.IMPULSIVA }
        val neutralCount = decisions.count { it.tone == DecisionTone.NEUTRA }

        val calmPercentage = calculatePercentage(calmCount, totalDecisions)
        val impulsivePercentage = calculatePercentage(impulsiveCount, totalDecisions)
        val neutralPercentage = calculatePercentage(neutralCount, totalDecisions)

        val dailyMoods = buildDailyMoods(decisions)
        val categoryDistribution = decisions.groupingBy { it.category }.eachCount()

        return WeeklySummary(
            startDate = startDate,
            endDate = endDate,
            totalDecisions = totalDecisions,
            calmPercentage = calmPercentage,
            impulsivePercentage = impulsivePercentage,
            neutralPercentage = neutralPercentage,
            dailyMoods = dailyMoods,
            categoryDistribution = categoryDistribution
        )
    }

    private fun calculatePercentage(count: Int, total: Int): Float {
        if (total == 0) return 0f
        return (count.toFloat() / total.toFloat()) * 100f
    }

    private fun buildDailyMoods(decisions: List<Decision>): Map<String, DailyMood> {
        if (decisions.isEmpty()) return emptyMap()

        val zoneId = ZoneId.systemDefault()
        val groupedByDate = decisions.groupBy { decision ->
            Instant.ofEpochMilli(decision.timestamp).atZone(zoneId).toLocalDate()
        }

        val result = linkedMapOf<String, DailyMood>()
        groupedByDate.entries
            .sortedBy { it.key }
            .forEach { (date, dayDecisions) ->
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                result[dayName] = calculateDailyMoodUseCase(dayDecisions)
            }

        return result
    }
}
