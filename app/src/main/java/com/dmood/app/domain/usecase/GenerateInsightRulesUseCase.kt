package com.dmood.app.domain.usecase

import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.EmotionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.util.Locale

data class InsightRuleResult(
    val title: String,
    val description: String,
    val tag: String
)

/**
 * Motor de reglas para generar insights cualitativos a partir de una lista de decisiones.
 *
 * Todas las reglas son deterministas: mismas decisiones -> mismos insights.
 * El foco está en describir lo que ocurre, no en dar consejos.
 */
class GenerateInsightRulesUseCase(
    @Suppress("unused")
    private val calculateDailyMoodUseCase: CalculateDailyMoodUseCase
) {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    operator fun invoke(decisions: List<Decision>): List<InsightRuleResult> {
        if (decisions.isEmpty()) return emptyList()

        val ctx = buildContext(decisions)
        val results = mutableListOf<InsightRuleResult>()

        applyGlobalEmotionRules(ctx, results)
        applyGlobalIntensityRules(ctx, results)
        applyEmotionDiversityRules(ctx, results)
        applyCategoryFocusRules(ctx, results)
        applyCategoryEmotionRules(ctx, results)
        applyCategoryIntensityRules(ctx, results)
        applyTemporalRules(ctx, results)
        applyCategoryUsageRules(ctx, results)

        return results.distinctBy { it.title }
    }

    // ---------- CONTEXTO COMPARTIDO ENTRE REGLAS ----------

    private data class RuleContext(
        val decisions: List<Decision>,
        val total: Int,
        val emotionCounts: Map<EmotionType, Int>,
        val decisionsByCategory: Map<CategoryType, List<Decision>>,
        val decisionsByDay: Map<LocalDate, List<Decision>>,
        val firstTimestamp: Long,
        val lastTimestamp: Long,
        val averageIntensity: Double,
        val highIntensityRatio: Double
    )

    private fun buildContext(decisions: List<Decision>): RuleContext {
        val total = decisions.size

        val emotionCounts = decisions
            .flatMap { it.emotions }
            .groupingBy { it }
            .eachCount()

        val decisionsByCategory = decisions.groupBy { it.category }

        val decisionsByDay = decisions.groupBy { decision ->
            Instant.ofEpochMilli(decision.timestamp).atZone(zoneId).toLocalDate()
        }

        val sortedByTime = decisions.sortedBy { it.timestamp }
        val firstTs = sortedByTime.first().timestamp
        val lastTs = sortedByTime.last().timestamp

        val averageIntensity = decisions.map { it.intensity }.average()
        val highIntensityRatio = if (total == 0) 0.0
        else decisions.count { it.intensity >= 4 }.toDouble() / total.toDouble()

        return RuleContext(
            decisions = decisions,
            total = total,
            emotionCounts = emotionCounts,
            decisionsByCategory = decisionsByCategory,
            decisionsByDay = decisionsByDay,
            firstTimestamp = firstTs,
            lastTimestamp = lastTs,
            averageIntensity = averageIntensity,
            highIntensityRatio = highIntensityRatio
        )
    }

    // ---------- EMOCIÓN GLOBAL ----------

    private fun applyGlobalEmotionRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        if (ctx.emotionCounts.isEmpty()) return

        val (dominantEmotion, count) = ctx.emotionCounts.maxByOrNull { it.value } ?: return
        val ratio = count.toFloat() / ctx.total.toFloat()
        val percentage = ratio.toPercentage()

        if (ratio >= 0.5f) {
            out += InsightRuleResult(
                title = "Emoción más presente: ${dominantEmotion.displayName}",
                description = "En aproximadamente $percentage de tus decisiones de esta semana está presente ${dominantEmotion.displayName.lowercase()}. " +
                        "Es el estado emocional que más ha acompañado tus elecciones.",
                tag = "Emoción dominante"
            )
        }
    }

    // ---------- INTENSIDAD GLOBAL ----------

    private fun applyGlobalIntensityRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        val avg = ctx.averageIntensity
        val highRatio = ctx.highIntensityRatio

        if (ctx.total < 3) return

        when {
            highRatio >= 0.5 -> {
                out += InsightRuleResult(
                    title = "Semana con muchas decisiones intensas",
                    description = "Una parte importante de tus decisiones (aprox. ${highRatio.toPercentage()}) se ha registrado con intensidad alta (≥ 4). " +
                            "La semana se ha vivido con bastante carga en el momento de decidir.",
                    tag = "Intensidad"
                )
            }

            avg <= 2.0 -> {
                out += InsightRuleResult(
                    title = "Semana de decisiones más suaves",
                    description = "La intensidad media de tus decisiones se sitúa alrededor de ${"%.1f".format(avg)} sobre 5. " +
                            "La mayor parte de elecciones se han percibido como poco intensas.",
                    tag = "Intensidad"
                )
            }
        }
    }

    // ---------- DIVERSIDAD EMOCIONAL ----------

    private fun applyEmotionDiversityRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        val distinctEmotions = ctx.emotionCounts.keys.size
        val totalPossible = EmotionType.values().size
        if (ctx.total < 4 || totalPossible == 0) return

        val ratio = distinctEmotions.toFloat() / totalPossible.toFloat()

        if (ratio <= 0.3f) {
            out += InsightRuleResult(
                title = "Te mueves entre pocas emociones",
                description = "Esta semana se han repetido sobre todo ${distinctEmotions} emociones distintas, sobre un total posible de $totalPossible. " +
                        "Tu paleta emocional registrada ha sido bastante concentrada.",
                tag = "Patrón emocional"
            )
        }
    }

    // ---------- CATEGORÍA MÁS PRESENTE ----------

    private fun applyCategoryFocusRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        if (ctx.decisionsByCategory.isEmpty()) return

        val (category, decisions) = ctx.decisionsByCategory.maxByOrNull { it.value.size } ?: return
        if (decisions.size < 2) return

        val ratio = decisions.size.toFloat() / ctx.total.toFloat()

        out += InsightRuleResult(
            title = "Área más presente: ${category.displayName}",
            description = "Aproximadamente ${ratio.toPercentage()} de tus decisiones han tenido que ver con ${category.displayName.lowercase()}. " +
                    "Es el tema que más ha aparecido en tu semana.",
            tag = "Áreas"
        )
    }

    // ---------- EMOCIÓN + CATEGORÍA ----------

    private fun applyCategoryEmotionRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        ctx.decisionsByCategory.forEach { (category, decisionsInCategory) ->
            if (decisionsInCategory.size < 3) return@forEach

            val emotionCounts = EmotionType.values().associateWith { emotion ->
                decisionsInCategory.count { decision -> emotion in decision.emotions }
            }

            val (dominantEmotion, count) = emotionCounts.maxByOrNull { it.value } ?: return@forEach
            if (count == 0) return@forEach

            val ratio = count.toFloat() / decisionsInCategory.size.toFloat()
            if (ratio < 0.6f) return@forEach

            val percentage = ratio.toPercentage()

            out += InsightRuleResult(
                title = "${dominantEmotion.displayName} en ${category.displayName}",
                description = "En torno al $percentage de las decisiones sobre ${category.displayName.lowercase()} aparecen asociadas a ${dominantEmotion.displayName.lowercase()}. " +
                        "Esta emoción está muy ligada a cómo decides en esa área.",
                tag = "Emoción en área"
            )
        }
    }

    // ---------- INTENSIDAD POR CATEGORÍA ----------

    private fun applyCategoryIntensityRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        ctx.decisionsByCategory.forEach { (category, decisionsInCategory) ->
            if (decisionsInCategory.size < 3) return@forEach

            val highIntensityCount = decisionsInCategory.count { it.intensity >= 4 }
            if (highIntensityCount == 0) return@forEach

            val ratio = highIntensityCount.toFloat() / decisionsInCategory.size.toFloat()
            if (ratio < 0.6f) return@forEach

            out += InsightRuleResult(
                title = "Intensidad alta en ${category.displayName}",
                description = "Una parte considerable de las decisiones sobre ${category.displayName.lowercase()} se ha registrado con intensidad alta (aprox. ${ratio.toPercentage()}). " +
                        "Esta área se vive con bastante intensidad al decidir.",
                tag = "Intensidad en área"
            )
        }
    }

    // ---------- PATRONES TEMPORALES ----------

    private fun applyTemporalRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        // Día con más carga negativa
        if (ctx.decisionsByDay.isNotEmpty()) {
            val negativeByDay = ctx.decisionsByDay.mapValues { (_, decisionsInDay) ->
                decisionsInDay.count { d -> d.emotions.any { it.isNegative() } }
            }

            val (worstDay, negativeCount) = negativeByDay.maxByOrNull { it.value } ?: return
            if (negativeCount >= 2) {
                val dayName = worstDay.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                out += InsightRuleResult(
                    title = "Día con más carga emocional negativa",
                    description = "El día de la semana con más decisiones asociadas a emociones negativas ha sido el $dayName.",
                    tag = "Ritmo semanal"
                )
            }
        }

        // Decisiones concentradas en poco tiempo (ventana temporal)
        val hoursBetween = ChronoUnit.HOURS.between(
            Instant.ofEpochMilli(ctx.firstTimestamp),
            Instant.ofEpochMilli(ctx.lastTimestamp)
        )

        if (ctx.total >= 5 && hoursBetween <= 48) {
            out += InsightRuleResult(
                title = "Muchas decisiones en poco tiempo",
                description = "Varias decisiones se han concentrado en un intervalo temporal inferior a 48 horas. " +
                        "La semana ha tenido un tramo especialmente denso en cuanto a elecciones.",
                tag = "Ritmo"
            )
        }
    }

    // ---------- CATEGORÍAS SIN DECISIONES ----------

    private fun applyCategoryUsageRules(ctx: RuleContext, out: MutableList<InsightRuleResult>) {
        if (ctx.total < 5) return

        val unusedCategories = CategoryType.values().filter { it !in ctx.decisionsByCategory.keys }
        if (unusedCategories.isEmpty()) return

        // Para no saturar, solo destacamos una
        val target = unusedCategories.first()

        out += InsightRuleResult(
            title = "Área sin decisiones: ${target.displayName}",
            description = "Esta semana no se han registrado decisiones relacionadas con ${target.displayName.lowercase()}. " +
                    "En los datos de la app, esa área aparece ausente.",
            tag = "Equilibrio"
        )
    }

    // ---------- HELPERS ----------

    private fun Float.toPercentage(): String = "${(this * 100).toInt()}%"

    private fun Double.toPercentage(): String = "${(this * 100).toInt()}%"
}
