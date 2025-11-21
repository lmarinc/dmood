package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType
import kotlin.math.roundToInt

/**
 * Resultado de una regla de insight aplicada sobre una colección de decisiones.
 */
data class DecisionInsight(
    val title: String,
    val headline: String,
    val supportingText: String
)

class GenerateDecisionInsightsUseCase {

    operator fun invoke(decisions: List<Decision>): List<DecisionInsight> {
        if (decisions.isEmpty()) return emptyList()

        val total = decisions.size.toFloat()
        val fearDrivenCount = decisions.count { it.emotions.contains(EmotionType.MIEDO) }
        val impulsiveCount = decisions.count { it.tone == DecisionTone.IMPULSIVA }
        val calmCount = decisions.count { it.tone == DecisionTone.CALMADA }
        val highIntensity = decisions.count { it.intensity >= 4 }

        val dominantCategory = decisions.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
        val distinctEmotions = decisions.flatMap { it.emotions }.distinct()
        val growthDecisions = decisions.count { it.category.isPersonalGrowthRelated() }

        val insights = mutableListOf<DecisionInsight>()

        if (fearDrivenCount / total >= 0.7f) {
            val percentage = (fearDrivenCount / total * 100).roundToInt()
            insights += DecisionInsight(
                title = "Foco en el miedo",
                headline = "$percentage% de tus decisiones estuvieron teñidas por el miedo",
                supportingText = "Observa qué situaciones lo detonan y busca un plan seguro antes de reaccionar."
            )
        }

        if (impulsiveCount >= calmCount * 2 && impulsiveCount >= 3) {
            insights += DecisionInsight(
                title = "Reacciones rápidas",
                headline = "Tus impulsos duplican a tus decisiones calmadas",
                supportingText = "Prueba a escribir lo que sientes antes de actuar: 2 minutos pueden darte claridad."
            )
        }

        if (calmCount / total >= 0.6f) {
            val percentage = (calmCount / total * 100).roundToInt()
            insights += DecisionInsight(
                title = "Ancla de serenidad",
                headline = "Mantienes la calma en el $percentage% de tus decisiones",
                supportingText = "Aprovecha este tono para planear retos grandes: ya tienes la base emocional."
            )
        }

        dominantCategory?.let { categoryEntry ->
            if (categoryEntry.value / total >= 0.5f) {
                insights += DecisionInsight(
                    title = "Áreas protagonistas",
                    headline = "${categoryEntry.key.displayName} concentra la mitad de tu semana",
                    supportingText = "Diversificar decisiones en otras áreas puede reducir tensión y darte balance."
                )
            }
        }

        if (highIntensity / total >= 0.6f) {
            insights += DecisionInsight(
                title = "Semana intensa",
                headline = "La mayoría de tus decisiones fueron de intensidad alta",
                supportingText = "Introduce pausas cortas antes de cada decisión para proteger tu energía."
            )
        }

        if (distinctEmotions.size <= 2) {
            insights += DecisionInsight(
                title = "Registro emocional corto",
                headline = "Solo has usado ${distinctEmotions.size} emociones esta semana",
                supportingText = "Nombrar emociones nuevas te ayudará a encontrar respuestas distintas."
            )
        }

        if (growthDecisions / total >= 0.5f) {
            insights += DecisionInsight(
                title = "Impulso de crecimiento",
                headline = "Más de la mitad de tus decisiones cuidaron de ti o tu progreso",
                supportingText = "Reconoce estos avances y celebra cómo se sienten en tu cuerpo."
            )
        }

        return insights.takeIf { it.isNotEmpty() }
            ?: listOf(
                DecisionInsight(
                    title = "Explora más",
                    headline = "Cuantas más decisiones registres, más preciso será tu mapa emocional",
                    supportingText = "Añade un par de decisiones cada día para desbloquear recomendaciones personalizadas."
                )
            )
    }
}
