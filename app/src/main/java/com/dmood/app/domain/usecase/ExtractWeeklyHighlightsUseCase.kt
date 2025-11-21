package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType

/**
 * Hallazgos cualitativos destacados sobre una semana.
 */
data class WeeklyHighlight(
    val strongestPositiveDay: String?,
    val strongestNegativeDay: String?,
    val mostFrequentCategory: CategoryType?,
    val emotionalTrend: String,
    val insights: List<WeeklyInsight>
)

data class WeeklyInsight(
    val title: String,
    val description: String,
    val badge: String? = null
)

/**
 * Caso de uso para extraer los puntos más relevantes de un WeeklySummary.
 */
class ExtractWeeklyHighlightsUseCase {

    operator fun invoke(summary: WeeklySummary): WeeklyHighlight {
        val strongestPositiveDay = findDayWithMood(summary, DailyMood.POSITIVO)
        val strongestNegativeDay = findDayWithMood(summary, DailyMood.NEGATIVO)

        val mostFrequentCategory = summary.categoryDistribution
            .maxByOrNull { it.value }
            ?.key

        val emotionalTrend = calculateTrend(summary)
        val insights = buildInsights(summary)

        return WeeklyHighlight(
            strongestPositiveDay = strongestPositiveDay,
            strongestNegativeDay = strongestNegativeDay,
            mostFrequentCategory = mostFrequentCategory,
            emotionalTrend = emotionalTrend,
            insights = insights
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

    private fun buildInsights(summary: WeeklySummary): List<WeeklyInsight> {
        val insights = mutableListOf<WeeklyInsight>()
        val totalEmotions = summary.emotionDistribution.values.sum().coerceAtLeast(1)
        val topEmotionEntry = summary.emotionDistribution.maxByOrNull { it.value }
        val topEmotionShare = topEmotionEntry?.let { (it.value.toFloat() / totalEmotions * 100).toInt() }

        if (topEmotionEntry != null && topEmotionShare != null) {
            val label = topEmotionEntry.key.displayName.lowercase()
            if (topEmotionShare >= 60) {
                insights += WeeklyInsight(
                    title = "Emoción dominante",
                    description = "El ${topEmotionShare}% de tus decisiones han estado teñidas de $label.",
                    badge = "Auto-chequeo"
                )
            }

            val fearShare = summary.emotionDistribution[com.dmood.app.domain.model.EmotionType.MIEDO]
                ?.let { (it.toFloat() / totalEmotions * 100).toInt() } ?: 0
            if (fearShare >= 50) {
                insights += WeeklyInsight(
                    title = "Decisiones desde el miedo",
                    description = "El ${fearShare}% de tus decisiones se originaron por miedo. Busca momentos seguros para decidir.",
                    badge = "Alerta"
                )
            }
        }

        val totalCategories = summary.categoryDistribution.values.sum().coerceAtLeast(1)
        summary.categoryDistribution.maxByOrNull { it.value }?.let { entry ->
            val percent = (entry.value.toFloat() / totalCategories * 100).toInt()
            if (percent >= 35) {
                insights += WeeklyInsight(
                    title = "Área protagonista",
                    description = "$percent% de tus decisiones han girado en torno a ${entry.key.displayName.lowercase()}.",
                    badge = "Foco"
                )
            }
        }

        if (summary.calmPercentage >= 60) {
            insights += WeeklyInsight(
                title = "Ritmo sereno",
                description = "${summary.calmPercentage.toInt()}% de tus decisiones fueron calmadas. ¡Sigue construyendo desde la calma!",
                badge = "Balance"
            )
        } else if (summary.impulsivePercentage >= 50) {
            insights += WeeklyInsight(
                title = "Impulso alto",
                description = "${summary.impulsivePercentage.toInt()}% de tus decisiones fueron impulsivas. Agenda pausas antes de decidir.",
                badge = "Acción"
            )
        }

        if (insights.isEmpty()) {
            insights += WeeklyInsight(
                title = "Explora tus registros",
                description = "Aún no hay un patrón dominante. Suma más decisiones esta semana para descubrir tendencias útiles.",
                badge = "Nuevo"
            )
        }

        return insights.take(4)
    }
}
