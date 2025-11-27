package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class InsightRuleResult(
    val title: String,
    val description: String,
    val tag: String
)

class GenerateInsightRulesUseCase(
    private val calculateDailyMoodUseCase: CalculateDailyMoodUseCase
) {

    operator fun invoke(
        decisions: List<Decision>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<InsightRuleResult> {
        if (decisions.isEmpty()) return emptyList()

        val total = decisions.size.toFloat()
        val emotionFrequency = EmotionType.values().associateWith { emotion ->
            decisions.count { it.emotions.contains(emotion) }
        }
        val toneCounts = decisions.groupingBy { it.tone }.eachCount()
        val averageIntensity = decisions.map { it.intensity }.average()
        val uniqueEmotions = emotionFrequency.count { it.value > 0 }
        val categoryGroups = decisions.groupBy { it.category }

        val prioritized = mutableListOf<PrioritizedInsight>()

        addCategoryEmotionInsights(categoryGroups, prioritized)
        addCategoryIntensityInsights(categoryGroups, prioritized)

        val mostUsedEmotion = emotionFrequency.maxByOrNull { it.value }
        mostUsedEmotion?.let { (emotion, count) ->
            val ratio = count / total
            if (ratio >= 0.7f) {
                prioritized += PrioritizedInsight(
                    priority = 3,
                    insight = InsightRuleResult(
                        title = "Decisiones impulsadas por ${emotion.displayName}",
                        description = "El ${ratio.toPercentage()} de tus decisiones han estado teñidas por ${emotion.displayName.lowercase()}.",
                        tag = "Emoción dominante"
                    )
                )
            }
        }

        val impulsiveRatio = (toneCounts[DecisionTone.IMPULSIVA] ?: 0) / total
        if (impulsiveRatio >= 0.55f) {
            prioritized += PrioritizedInsight(
                priority = 4,
                insight = InsightRuleResult(
                    title = "Predominio impulsivo",
                    description = "${(impulsiveRatio * 100).toInt()}% de las decisiones nacieron desde un impulso. Date un respiro antes de decidir.",
                    tag = "Autocontrol"
                )
            )
        }

        val calmRatio = (toneCounts[DecisionTone.CALMADA] ?: 0) / total
        if (calmRatio >= 0.6f) {
            prioritized += PrioritizedInsight(
                priority = 5,
                insight = InsightRuleResult(
                    title = "Fortaleza serena",
                    description = "${(calmRatio * 100).toInt()}% de tus decisiones se tomaron con calma. Mantén esos rituales que te centran.",
                    tag = "Serenidad"
                )
            )
        }

        if (averageIntensity >= 4.2) {
            prioritized += PrioritizedInsight(
                priority = 4,
                insight = InsightRuleResult(
                    title = "Semana intensa",
                    description = "Tu media de intensidad ha sido ${"%.1f".format(averageIntensity)} / 5. Reduce ruido planificando microdescansos.",
                    tag = "Energía"
                )
            )
        }

        val topCategory = categoryGroups.maxByOrNull { it.value.size }
        topCategory?.let { (category, list) ->
            val weight = list.size / total
            if (weight >= 0.5f) {
                prioritized += PrioritizedInsight(
                    priority = 6,
                    insight = InsightRuleResult(
                        title = "Enfoque en ${category.displayName}",
                        description = "La mitad de tus decisiones están conectadas con ${category.displayName.lowercase()}. Agenda tiempo específico para ello.",
                        tag = "Prioridad"
                    )
                )
            }
        }

        if (uniqueEmotions <= 2) {
            prioritized += PrioritizedInsight(
                priority = 6,
                insight = InsightRuleResult(
                    title = "Paleta emocional limitada",
                    description = "Solo has navegado por $uniqueEmotions emociones principales. Explora qué otras sensaciones se esconden detrás de cada decisión.",
                    tag = "Autoconocimiento"
                )
            )
        }

        val lastTwoDaysCount = decisions.count { decision ->
            val date = decision.timestamp.toLocalDate(zoneId)
            val maxDate = decisions.maxOf { it.timestamp }.toLocalDate(zoneId)
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(date, maxDate)
            daysDiff in 0..1
        }
        if (lastTwoDaysCount / total >= 0.5f) {
            prioritized += PrioritizedInsight(
                priority = 6,
                insight = InsightRuleResult(
                    title = "Decisiones concentradas al final",
                    description = "Más de la mitad de tus decisiones se acumularon en las últimas 48h. Reserva espacios regulares para decidir con claridad.",
                    tag = "Ritmo"
                )
            )
        }

        val moodByDay = decisions.groupBy { it.timestamp.toLocalDate(zoneId) }
            .mapValues { (_, dayDecisions) -> calculateDailyMoodUseCase(dayDecisions) }
        val negativeStreak = moodByDay.filterValues { it == DailyMood.NEGATIVO }
        if (negativeStreak.isNotEmpty()) {
            val worstDay = negativeStreak.keys.maxByOrNull { it }?.dayOfWeek
            worstDay?.let { day ->
                prioritized += PrioritizedInsight(
                    priority = 5,
                    insight = InsightRuleResult(
                        title = "Día más retador",
                        description = "Tus emociones se tensan los ${day.getDisplayName(TextStyle.FULL, Locale("es", "ES"))}. Planifica algo ligero ese día.",
                        tag = "Prevención"
                    )
                )
            }
        }

        return prioritized
            .sortedWith(compareBy<PrioritizedInsight> { it.priority }.thenByDescending { it.weight })
            .map { it.insight }
            .distinctBy { it.title }
    }
}

private fun Float.toPercentage(): String = "${(this * 100).toInt()}%"

private fun Long.toLocalDate(zoneId: ZoneId): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private data class PrioritizedInsight(
    val priority: Int,
    val insight: InsightRuleResult,
    val weight: Float = 0f
)

private fun addCategoryEmotionInsights(
    categoryGroups: Map<com.dmood.app.domain.model.CategoryType, List<Decision>>,
    prioritized: MutableList<PrioritizedInsight>
) {
    categoryGroups.forEach { (category, decisions) ->
        val totalCategory = decisions.size
        if (totalCategory < 3) return@forEach

        val emotionPresence = EmotionType.values().associateWith { emotion ->
            decisions.count { it.emotions.contains(emotion) }
        }

        emotionPresence.forEach { (emotion, count) ->
            val ratio = count / totalCategory.toFloat()
            if (ratio >= 0.7f && emotion.isNegative()) {
                prioritized += PrioritizedInsight(
                    priority = 1,
                    weight = ratio,
                    insight = InsightRuleResult(
                        title = "${emotion.displayName} manda en ${category.displayName}",
                        description = "El ${ratio.toPercentage()} de tus decisiones sobre ${category.displayName.lowercase()} han estado teñidas por ${emotion.displayName.lowercase()}. Piensa qué cambiaría si el ${emotion.displayName.lowercase()} no liderara.",
                        tag = "Emoción dominante"
                    )
                )
            } else if (ratio >= 0.6f && emotion.isPositive()) {
                prioritized += PrioritizedInsight(
                    priority = 2,
                    weight = ratio,
                    insight = InsightRuleResult(
                        title = "${emotion.displayName} guía tus ${category.displayName.lowercase()}",
                        description = "En ${ratio.toPercentage()} de decisiones sobre ${category.displayName.lowercase()} apareció ${emotion.displayName.lowercase()}. Potencia esos momentos para avanzar.",
                        tag = "Refuerzo"
                    )
                )
            }
        }
    }
}

private fun addCategoryIntensityInsights(
    categoryGroups: Map<com.dmood.app.domain.model.CategoryType, List<Decision>>,
    prioritized: MutableList<PrioritizedInsight>
) {
    categoryGroups.forEach { (category, decisions) ->
        val totalCategory = decisions.size
        if (totalCategory == 0) return@forEach

        val highIntensityCount = decisions.count { it.intensity >= 4 }
        val highIntensityRatio = highIntensityCount / totalCategory.toFloat()

        val negativeEmotionPresence = decisions.count { decision ->
            decision.emotions.any { it.isNegative() }
        } / totalCategory.toFloat()

        if (highIntensityRatio >= 0.5f && negativeEmotionPresence >= 0.5f) {
            prioritized += PrioritizedInsight(
                priority = 1,
                weight = (highIntensityRatio + negativeEmotionPresence) / 2f,
                insight = InsightRuleResult(
                    title = "${category.displayName}: zona caliente",
                    description = "El ${highIntensityRatio.toPercentage()} de tus decisiones intensas en ${category.displayName.lowercase()} llegaron con emociones tensas. Busca un ritual de pausa antes de decidir ahí.",
                    tag = "Intensidad"
                )
            )
        }
    }
}
