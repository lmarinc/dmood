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
        summary: WeeklySummary,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<InsightRuleResult> {
        if (decisions.isEmpty()) return emptyList()

        val total = decisions.size.toFloat()
        val toneCounts = decisions.groupingBy { it.tone }.eachCount()
        val averageIntensity = decisions.map { it.intensity }.average()
        val rules = mutableListOf<InsightRuleResult>()

        rules += dominantEmotionInsight(summary, total)

        val impulsiveRatio = (toneCounts[DecisionTone.IMPULSIVA] ?: 0) / total
        if (impulsiveRatio >= 0.55f) {
            rules += InsightRuleResult(
                title = "Estás decidiendo en automático",
                description = "En ${impulsiveRatio.toPercentage()} de tus decisiones no hubo pausa. Antes de la próxima decisión intensa, frena 10 segundos y respira.",
                tag = "Autocontrol"
            )
        }

        rules += dominantEmotionByCategory(summary)

        val impulsiveEmotionsRule = impulsiveEmotionRule(decisions)
        if (impulsiveEmotionsRule != null) rules += impulsiveEmotionsRule

        if (averageIntensity >= 4.2) {
            rules += InsightRuleResult(
                title = "Semana a todo volumen",
                description = "Tu media de intensidad fue ${"%.1f".format(averageIntensity)} / 5. Baja revoluciones agendando micro-pausas antes de decidir.",
                tag = "Gestión de energía"
            )
        }

        val uniqueEmotions = summary.emotionDistribution.count { it.value > 0 }
        if (uniqueEmotions <= 3) {
            rules += InsightRuleResult(
                title = "Siempre en los mismos estados",
                description = "Has navegado casi siempre por ${uniqueEmotions.coerceAtLeast(1)} emociones. Ponle nombre más preciso a lo que sientes para abrir opciones.",
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
                title = "Todo al final",
                description = "Más de la mitad de tus decisiones se amontonaron en las últimas 48h. Bloquea 15 minutos cada día para decidir con la cabeza fría.",
                tag = "Ritmo"
            )
        }

        val worstDayRule = challengingDayRule(decisions, zoneId)
        if (worstDayRule != null) rules += worstDayRule

        val calmRatio = (toneCounts[DecisionTone.CALMADA] ?: 0) / total
        if (calmRatio >= 0.6f) {
            rules += InsightRuleResult(
                title = "La calma sigue ahí",
                description = "${calmRatio.toPercentage()} de tus decisiones nacieron desde la serenidad. Conserva los rituales que te ayudan a bajar pulsaciones.",
                tag = "Serenidad"
            )
        }

        return rules.distinctBy { it.title }
    }

    // Emoción dominante global con copy más directo.
    private fun dominantEmotionInsight(summary: WeeklySummary, total: Float): List<InsightRuleResult> {
        if (summary.emotionDistribution.isEmpty() || total == 0f) return emptyList()

        val dominant = summary.emotionDistribution.maxByOrNull { it.value } ?: return emptyList()
        val ratio = dominant.value / total

        return if (ratio >= 0.6f) {
            listOf(
                InsightRuleResult(
                    title = "${dominant.key.displayName} está al mando",
                    description = "${ratio.toPercentage()} de tus decisiones pasaron por el filtro de ${dominant.key.displayName.lowercase(Locale.getDefault())}. Pregúntate qué eliges cuando esa emoción no decide por ti.",
                    tag = "Emoción dominante"
                )
            )
        } else {
            emptyList()
        }
    }

    // Detecta emociones dominantes por categoría (ej. miedo en trabajo >= 70%).
    private fun dominantEmotionByCategory(summary: WeeklySummary): List<InsightRuleResult> {
        if (summary.categoryEmotionMatrix.isEmpty()) return emptyList()

        return summary.categoryEmotionMatrix.mapNotNull { (category, emotions) ->
            val categoryTotal = emotions.values.sum()
            if (categoryTotal < 3) return@mapNotNull null

            val dominant = emotions.maxByOrNull { it.value } ?: return@mapNotNull null
            val ratio = dominant.value / categoryTotal.toFloat()

            if (ratio >= 0.7f) {
                InsightRuleResult(
                    title = "${dominant.key.displayName} manda en tu ${category.displayName.lowercase()}",
                    description = "${ratio.toPercentage()} de tus decisiones sobre ${category.displayName.lowercase()} estuvieron teñidas por ${dominant.key.displayName.lowercase()}. Elige una y pregúntate: ¿qué decidirías si esa emoción no llevara el volante?",
                    tag = "Emoción dominante en categoría"
                )
            } else {
                null
            }
        }
    }

    // Cuántas decisiones impulsivas están cargadas de emociones negativas.
    private fun impulsiveEmotionRule(decisions: List<Decision>): InsightRuleResult? {
        val impulsiveDecisions = decisions.filter { it.tone == DecisionTone.IMPULSIVA }
        if (impulsiveDecisions.isEmpty()) return null

        val impulsiveWithNegative = impulsiveDecisions.count { decision ->
            decision.emotions.any { it.isNegative() }
        }
        val ratio = impulsiveWithNegative / impulsiveDecisions.size.toFloat()

        return if (ratio >= 0.6f) {
            InsightRuleResult(
                title = "Impulsos teñidos de emociones pesadas",
                description = "El ${ratio.toPercentage()} de tus decisiones impulsivas vinieron con miedo, ira o incomodidad. Cuando notes el pico, escribe la emoción antes de actuar.",
                tag = "Impulso emocional"
            )
        } else {
            null
        }
    }

    // Día más retador incluyendo la emoción predominante.
    private fun challengingDayRule(
        decisions: List<Decision>,
        zoneId: ZoneId
    ): InsightRuleResult? {
        val moodByDay = decisions.groupBy { it.timestamp.toLocalDate(zoneId) }
            .mapValues { (_, dayDecisions) -> calculateDailyMoodUseCase(dayDecisions) }
        val negativeStreak = moodByDay.filterValues { it == DailyMood.NEGATIVO }
        val worstDay = negativeStreak.keys.maxByOrNull { it } ?: return null

        val emotionForDay = decisions
            .filter { it.timestamp.toLocalDate(zoneId) == worstDay }
            .flatMap { it.emotions }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val dayName = worstDay.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        val emotionName = emotionForDay?.displayName?.lowercase(Locale.getDefault()) ?: "emociones tensas"

        return InsightRuleResult(
            title = "Tus ${dayName.lowercase(Locale.getDefault())} pesan más",
            description = "Los ${dayName.lowercase(Locale.getDefault())} se te atragantan, sobre todo con ${emotionName}. Evita decisiones clave ese día o déjalas preparadas desde antes.",
            tag = "Prevención"
        )
    }
}

private fun Float.toPercentage(): String = "${(this * 100).toInt()}%"

private fun Long.toLocalDate(zoneId: ZoneId): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

