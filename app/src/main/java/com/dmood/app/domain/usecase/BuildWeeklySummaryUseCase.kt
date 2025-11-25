package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Resumen cuantitativo de una semana de decisiones y estados emocionales.
 */
data class WeeklySummary(
    val startDate: Long,
    val endDate: Long,
    val totalDecisions: Int,
    val calmPercentage: Float,
    val impulsivePercentage: Float,
    val neutralPercentage: Float,
    val dailyMoods: Map<String, DailyMood>,
    val categoryDistribution: Map<CategoryType, Int>,
    val emotionDistribution: Map<EmotionType, Int>,
    val categoryEmotionMatrix: Map<CategoryType, Map<EmotionType, Int>>,
    val toneEmotionDistribution: Map<DecisionTone, Map<EmotionType, Int>>
)

/**
 * Caso de uso para construir el resumen semanal a partir de las decisiones
 * registradas en un intervalo de fechas.
 */
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
        val emotionDistribution = buildEmotionDistribution(decisions)
        val categoryEmotionMatrix = buildCategoryEmotionMatrix(decisions)
        val toneEmotionDistribution = buildToneEmotionDistribution(decisions)

        return WeeklySummary(
            startDate = startDate,
            endDate = endDate,
            totalDecisions = totalDecisions,
            calmPercentage = calmPercentage,
            impulsivePercentage = impulsivePercentage,
            neutralPercentage = neutralPercentage,
            dailyMoods = dailyMoods,
            categoryDistribution = categoryDistribution,
            emotionDistribution = emotionDistribution,
            categoryEmotionMatrix = categoryEmotionMatrix,
            toneEmotionDistribution = toneEmotionDistribution
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
            Instant.ofEpochMilli(decision.timestamp)
                .atZone(zoneId)
                .toLocalDate()
        }

        val result = linkedMapOf<String, DailyMood>()

        groupedByDate.entries
            .sortedBy { it.key }
            .forEach { (date, dayDecisions) ->
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                result[dayName] = calculateDailyMoodUseCase(dayDecisions)
            }

        return result
    }

    // Conteo total de emociones en la semana.
    private fun buildEmotionDistribution(decisions: List<Decision>): Map<EmotionType, Int> {
        if (decisions.isEmpty()) return emptyMap()

        return EmotionType.values().associateWith { emotion ->
            decisions.count { decision -> decision.emotions.contains(emotion) }
        }
    }

    // Matriz categoría-emoción: cuántas veces aparece cada emoción en cada categoría.
    private fun buildCategoryEmotionMatrix(
        decisions: List<Decision>
    ): Map<CategoryType, Map<EmotionType, Int>> {
        if (decisions.isEmpty()) return emptyMap()

        return decisions.groupBy { it.category }.mapValues { (_, categoryDecisions) ->
            EmotionType.values().associateWith { emotion ->
                categoryDecisions.count { decision -> decision.emotions.contains(emotion) }
            }
        }
    }

    // Distribución de emociones según el tono de la decisión (calma vs impulsiva vs neutra).
    private fun buildToneEmotionDistribution(
        decisions: List<Decision>
    ): Map<DecisionTone, Map<EmotionType, Int>> {
        if (decisions.isEmpty()) return emptyMap()

        return DecisionTone.values().associateWith { tone ->
            val toneDecisions = decisions.filter { it.tone == tone }
            EmotionType.values().associateWith { emotion ->
                toneDecisions.count { decision -> decision.emotions.contains(emotion) }
            }
        }
    }
}
