package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.domain.usecase.DailyMood
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

        val mostUsedEmotion = emotionFrequency.maxByOrNull { it.value }
        val uniqueEmotions = emotionFrequency.count { it.value > 0 }

        val rules = mutableListOf<InsightRuleResult>()

        mostUsedEmotion?.let { (emotion, count) ->
            val ratio = count / total
            if (ratio >= 0.7f) {
                rules += InsightRuleResult(
                    title = "Decisiones impulsadas por ${emotion.displayName}",
                    description = "El ${ratio.toPercentage()} de tus decisiones han estado teñidas por ${emotion.displayName.lowercase()}.",
                    tag = "Emoción dominante"
                )
            }
        }

        val impulsiveRatio = (toneCounts[DecisionTone.IMPULSIVA] ?: 0) / total
        if (impulsiveRatio >= 0.55f) {
            rules += InsightRuleResult(
                title = "Predominio impulsivo",
                description = "${(impulsiveRatio * 100).toInt()}% de las decisiones nacieron desde un impulso. Date un respiro antes de decidir.",
                tag = "Autocontrol"
            )
        }

        val calmRatio = (toneCounts[DecisionTone.CALMADA] ?: 0) / total
        if (calmRatio >= 0.6f) {
            rules += InsightRuleResult(
                title = "Fortaleza serena",
                description = "${(calmRatio * 100).toInt()}% de tus decisiones se tomaron con calma. Mantén esos rituales que te centran.",
                tag = "Serenidad"
            )
        }

        if (averageIntensity >= 4.2) {
            rules += InsightRuleResult(
                title = "Semanas intensas",
                description = "Tu media de intensidad ha sido ${"%.1f".format(averageIntensity)} / 5. Reduce ruido planificando microdescansos.",
                tag = "Gestión de energía"
            )
        }

        val topCategory = decisions.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
        topCategory?.let { (category, count) ->
            val weight = count / total
            if (weight >= 0.5f) {
                rules += InsightRuleResult(
                    title = "Enfoque en ${category.displayName}",
                    description = "La mitad de tus decisiones están conectadas con ${category.displayName.lowercase()}. Agenda tiempo específico para ello.",
                    tag = "Prioridad"
                )
            }
        }

        if (uniqueEmotions <= 2) {
            rules += InsightRuleResult(
                title = "Paleta emocional limitada",
                description = "Solo has navegado por $uniqueEmotions emociones principales. Explora qué otras sensaciones se esconden detrás de cada decisión.",
                tag = "Autoconocimiento"
            )
        }

        val lastTwoDaysCount = decisions.count { decision ->
            val date = decision.timestamp.toLocalDate(zoneId)
            val maxDate = decisions.maxOf { it.timestamp }.toLocalDate(zoneId)
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(date, maxDate)
            daysDiff in 0..1
        }
        if (lastTwoDaysCount / total >= 0.5f) {
            rules += InsightRuleResult(
                title = "Decisiones concentradas al final",
                description = "Más de la mitad de tus decisiones se acumularon en las últimas 48h. Reserva espacios regulares para decidir con claridad.",
                tag = "Ritmo"
            )
        }

        val moodByDay = decisions.groupBy { it.timestamp.toLocalDate(zoneId) }
            .mapValues { (_, dayDecisions) -> calculateDailyMoodUseCase(dayDecisions) }
        val negativeStreak = moodByDay.filterValues { it == DailyMood.NEGATIVO }
        if (negativeStreak.isNotEmpty()) {
            val worstDay = negativeStreak.keys.maxByOrNull { it }?.dayOfWeek
            worstDay?.let { day ->
                rules += InsightRuleResult(
                    title = "Día más retador",
                    description = "Tus emociones se tensan los ${day.getDisplayName(TextStyle.FULL, Locale("es", "ES"))}. Planifica algo ligero ese día.",
                    tag = "Prevención"
                )
            }
        }

        return rules.distinctBy { it.title }
    }
}

private fun Float.toPercentage(): String = "${(this * 100).toInt()}%"

private fun Long.toLocalDate(zoneId: ZoneId): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

