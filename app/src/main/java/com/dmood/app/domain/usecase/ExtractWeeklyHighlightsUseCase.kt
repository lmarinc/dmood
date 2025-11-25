package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.EmotionType
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Hallazgos cualitativos destacados sobre una semana.
 */
data class WeeklyHighlight(
    val strongestPositiveDay: String?,
    val strongestNegativeDay: String?,
    val mostFrequentCategory: CategoryType?,
    val emotionalTrend: String,
    val mostChallengingDayEmotion: EmotionType?
)

/**
 * Caso de uso para extraer los puntos más relevantes de un WeeklySummary.
 */
class ExtractWeeklyHighlightsUseCase {

    operator fun invoke(
        summary: WeeklySummary,
        decisions: List<Decision>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeeklyHighlight {
        val strongestPositiveDay = findDayWithMood(summary, DailyMood.POSITIVO)
        val challengingDayInfo = findMostChallengingDay(decisions, zoneId)
        val strongestNegativeDay = challengingDayInfo?.first ?: findDayWithMood(summary, DailyMood.NEGATIVO)
        val challengingDayEmotion = challengingDayInfo?.second

        val mostFrequentCategory = summary.categoryDistribution
            .maxByOrNull { it.value }
            ?.key

        val emotionalTrend = calculateTrend(summary)

        return WeeklyHighlight(
            strongestPositiveDay = strongestPositiveDay,
            strongestNegativeDay = strongestNegativeDay,
            mostFrequentCategory = mostFrequentCategory,
            emotionalTrend = emotionalTrend,
            mostChallengingDayEmotion = challengingDayEmotion
        )
    }

    /**
     * Devuelve el primer día que presenta el estado de ánimo indicado.
     * (Simplificación: día representativo, no el “más fuerte” a nivel cuantitativo.)
     */
    private fun findDayWithMood(summary: WeeklySummary, mood: DailyMood): String? {
        return summary.dailyMoods.entries.firstOrNull { it.value == mood }?.key
    }

    private fun calculateTrend(summary: WeeklySummary): String {
        val positiveDays = summary.dailyMoods.count { it.value == DailyMood.POSITIVO }
        val negativeDays = summary.dailyMoods.count { it.value == DailyMood.NEGATIVO }

        return when {
            positiveDays > negativeDays -> "Semana predominantemente positiva"
            negativeDays > positiveDays -> "Semana predominantemente negativa"
            else -> "Semana equilibrada"
        }
    }

    private fun findMostChallengingDay(
        decisions: List<Decision>,
        zoneId: ZoneId
    ): Pair<String, EmotionType?>? {
        if (decisions.isEmpty()) return null

        val grouped = decisions.groupBy { decision ->
            java.time.Instant.ofEpochMilli(decision.timestamp).atZone(zoneId).toLocalDate()
        }

        val mostChallenging = grouped.maxByOrNull { (_, dayDecisions) ->
            // Peso por decisiones con emociones negativas o intensidades altas.
            dayDecisions.count { it.emotions.any(EmotionType::isNegative) || it.intensity >= 4 }
        } ?: return null

        val dayName = mostChallenging.key.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        val dominantEmotion = mostChallenging.value
            .flatMap { it.emotions }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        return dayName to dominantEmotion
    }
}
