package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklySummaryViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWeeklySummary()
    }

    val weekRange = uiState.summary?.let { formatWeekRange(it) }
    val nextSummaryFriendly = uiState.nextSummaryDate?.format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
    )?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

    val heroInsight = uiState.heroInsight ?: uiState.insights.firstOrNull()

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                subtitle = "Un espejo emocional",
                showLogo = true,
                onBack = onBack
            )
        }
    ) { padding ->
        val baseModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)

        when {
            uiState.isLoading -> {
                Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.summary == null -> {
                SummaryStatusCard(
                    userName = uiState.userName,
                    weekRange = weekRange,
                    nextSummaryFriendly = nextSummaryFriendly,
                    isSummaryAvailable = uiState.isSummaryAvailable || uiState.isDemo,
                    isLoading = uiState.isLoading,
                    isDemo = uiState.isDemo,
                    onActionClick = {
                        if (uiState.isSummaryAvailable) {
                            viewModel.loadWeeklySummary()
                        } else {
                            viewModel.loadDemoSummary()
                        }
                    }
                )
            }

            else -> {
                WeeklySummaryContent(
                    summary = uiState.summary,
                    heroInsight = heroInsight,
                    insights = uiState.insights,
                    weekRange = weekRange,
                    highlight = uiState.highlight
                )
            }
        }
    }
}

@Composable
private fun WeeklySummaryContent(
    summary: WeeklySummary,
    heroInsight: InsightRuleResult?,
    insights: List<InsightRuleResult>,
    weekRange: String?,
    highlight: WeeklyHighlight?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        weekRange?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HeroInsightCard(heroInsight)

        ToneDistributionCard(
            calm = summary.calmPercentage,
            impulsive = summary.impulsivePercentage,
            neutral = summary.neutralPercentage
        )

        highlight?.let { ChallengingDayCard(it) }

        CategoryEmotionSection(summary)

        InsightListSection(
            insights = insights,
            heroInsight = heroInsight
        )
    }
}

@Composable
private fun HeroInsightCard(heroInsight: InsightRuleResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tema de la semana",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (heroInsight != null) {
                val percentageText = extractPercentage(heroInsight)

                Text(
                    text = percentageText,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = heroInsight.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = heroInsight.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Aún no hay un insight dominante esta semana.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ToneDistributionCard(
    calm: Float,
    impulsive: Float,
    neutral: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Cómo decidiste", style = MaterialTheme.typography.titleMedium)
            ToneRow(label = "Calma", value = calm, color = Color(0xFF69C7A1))
            ToneRow(label = "Impulso", value = impulsive, color = Color(0xFFFF9D6C))
            ToneRow(label = "Neutro", value = neutral, color = MaterialTheme.colorScheme.outline)
            Text(
                text = "Si notas que decides en automático, pausa 10 segundos antes de la siguiente decisión.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ToneRow(label: String, value: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "$label · ${value.toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ChallengingDayCard(highlight: WeeklyHighlight) {
    if (highlight.strongestNegativeDay == null && highlight.mostChallengingDayEmotion == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Día que más pesa", style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildString {
                    highlight.strongestNegativeDay?.let { append("Tus $it se te atragantan.") }
                    highlight.mostChallengingDayEmotion?.let {
                        if (isNotEmpty()) append(" ")
                        append("Predomina ${it.displayName.lowercase()} ese día.")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Evita decisiones clave ese día o déjalas preparadas la tarde anterior.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryEmotionSection(summary: WeeklySummary) {
    val categoryInfo = buildCategoryEmotionInfo(summary)
    if (categoryInfo.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Dónde pasa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        categoryInfo.forEach { info ->
            CategoryEmotionCard(info)
        }
    }
}

@Composable
private fun CategoryEmotionCard(info: CategoryEmotionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = info.category.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${info.total} decisiones · ${info.emotionLabel}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "La emoción que más aparece: ${info.emotionLabel}. Cambia una decisión en esta área probando otro tono emocional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightListSection(
    insights: List<InsightRuleResult>,
    heroInsight: InsightRuleResult?
) {
    val additionalInsights = insights.filter { it != heroInsight }
    Text(
        text = "Lo que esta semana dice de ti",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    if (additionalInsights.isEmpty()) {
        Text(
            text = "Aún no hemos detectado patrones adicionales.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            additionalInsights.forEach { insight ->
                InsightCard(insight)
            }
        }
    }
}

@Composable
private fun InsightCard(insight: InsightRuleResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = insight.tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class CategoryEmotionInfo(
    val category: CategoryType,
    val total: Int,
    val dominantEmotion: EmotionType?,
    val dominantPercentage: Int
) {
    val emotionLabel: String
        get() = dominantEmotion?.let { "${it.displayName.lowercase()} (${dominantPercentage}%)" } ?: "sin emoción clara"
}

private fun buildCategoryEmotionInfo(summary: WeeklySummary): List<CategoryEmotionInfo> {
    if (summary.categoryEmotionMatrix.isEmpty()) return emptyList()

    return summary.categoryEmotionMatrix.mapNotNull { (category, emotions) ->
        val total = emotions.values.sum()
        if (total == 0) return@mapNotNull null
        val dominant = emotions.maxByOrNull { it.value }
        val percentage = dominant?.let { ((it.value / total.toFloat()) * 100).toInt() } ?: 0
        CategoryEmotionInfo(category = category, total = total, dominantEmotion = dominant?.key, dominantPercentage = percentage)
    }.sortedByDescending { it.total }
}

private fun extractPercentage(insight: InsightRuleResult): String {
    val match = "(\\d+%)".toRegex().find(insight.description) ?: "(\\d+%)".toRegex().find(insight.title)
    return match?.value ?: ""
}

private fun formatWeekRange(summary: WeeklySummary): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(summary.startDate).atZone(zone).toLocalDate()
    val end = Instant.ofEpochMilli(summary.endDate).atZone(zone).toLocalDate()
    return "Del ${start.format(formatter)} al ${end.format(formatter)}"
}

@Composable
private fun SummaryStatusCard(
    userName: String?,
    weekRange: String?,
    nextSummaryFriendly: String?,
    isSummaryAvailable: Boolean,
    isLoading: Boolean,
    isDemo: Boolean,
    onActionClick: () -> Unit
) {
    val headline = userName?.let { "Tu semana, $it" } ?: "Tu semana"
    val description = when {
        isLoading -> "Preparando tu resumen..."
        isSummaryAvailable && !isDemo -> "El resumen de esta semana está listo para ti."
        isDemo -> "Aún no tienes resumen semanal, aquí tienes una demostración."
        else -> "Aún no tienes resumen semanal, aquí te dejo una demostración para que veas lo que te espera."
    }
    val actionLabel = when {
        isDemo -> "Empezar Demo"
        isSummaryAvailable -> "Empezar"
        else -> "Empezar Demo"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )
            weekRange?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            nextSummaryFriendly?.let {
                Text(
                    text = "Próximo resumen: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.material3.Button(onClick = onActionClick) {
                Text(text = actionLabel)
            }
        }
    }
}
