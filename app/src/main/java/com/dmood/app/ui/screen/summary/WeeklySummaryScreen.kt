package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.usecase.InsightRuleResult
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
    var hasStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadWeeklySummary()
    }

    LaunchedEffect(uiState.isSummaryAvailable, uiState.isDemo) {
        if (!uiState.isSummaryAvailable && !uiState.isDemo) {
            hasStarted = false
        }
    }

    val weekRange = uiState.summary?.let { summary ->
        val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(summary.startDate).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(summary.endDate).atZone(zone).toLocalDate()
        "Del ${start.format(formatter)} al ${end.format(formatter)}"
    }
    val nextSummaryFriendly = uiState.nextSummaryDate?.format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
    )?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

    val summarySteps = remember(uiState.summary, uiState.highlight, uiState.insights) {
        buildList<SummaryStep> {
            uiState.highlight?.let { highlight ->
                add(SummaryStep.Overview(trend = highlight.emotionalTrend))
                add(SummaryStep.Highlight(highlight = highlight))
            }

            uiState.summary?.let { summary ->
                add(
                    SummaryStep.Tone(
                        calm = summary.calmPercentage ?: 0f,
                        impulsive = summary.impulsivePercentage ?: 0f
                    )
                )

                (summary.categoryDistribution ?: emptyMap()).entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .forEach { entry ->
                        add(
                            SummaryStep.Category(
                                entry = entry,
                                total = summary.totalDecisions ?: 0
                            )
                        )
                    }
            }

            if (uiState.insights.isNotEmpty()) {
                add(SummaryStep.Insights(uiState.insights))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                subtitle = "Repasa tu progreso",
                showLogo = true
            )
        }
    ) { innerPadding ->
        val baseModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp)

        when {
            uiState.isLoading -> {
                Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !hasStarted -> {
                Box(
                    modifier = baseModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryStatusCard(
                            userName = uiState.userName,
                            weekRange = weekRange,
                            nextSummaryFriendly = nextSummaryFriendly,
                            isSummaryAvailable = uiState.isSummaryAvailable || uiState.isDemo,
                            isLoading = uiState.isLoading,
                            isDemo = uiState.isDemo,
                            onActionClick = {
                                if (uiState.isSummaryAvailable) {
                                    hasStarted = summarySteps.isNotEmpty()
                                } else {
                                    viewModel.loadDemoSummary()
                                    hasStarted = true
                                }
                            }
                        )

                        Text(
                            text = uiState.errorMessage
                                ?: "Cuando tu resumen esté listo podrás iniciarlo desde aquí.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            summarySteps.isNotEmpty() -> {
                SummaryStepsPager(
                    steps = summarySteps,
                    modifier = baseModifier
                )
            }

            else -> {
                Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no hay información para mostrar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private sealed class SummaryStep {
    data class Overview(val trend: String) : SummaryStep()
    data class Tone(val calm: Float, val impulsive: Float) : SummaryStep()
    data class Highlight(val highlight: com.dmood.app.domain.usecase.WeeklyHighlight) : SummaryStep()
    data class Category(val entry: Map.Entry<CategoryType, Int>, val total: Int) : SummaryStep()
    data class Insights(val insights: List<InsightRuleResult>) : SummaryStep()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummaryStepsPager(
    steps: List<SummaryStep>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val stepCardModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 260.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            contentPadding = PaddingValues(horizontal = 8.dp),
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { page ->
            when (val step = steps[page]) {
                is SummaryStep.Overview -> SummaryCard(
                    title = "Tono de la semana",
                    description = step.trend,
                    modifier = stepCardModifier
                )

                is SummaryStep.Tone -> ToneDistributionCard(
                    calm = step.calm,
                    impulsive = step.impulsive,
                    modifier = stepCardModifier
                )

                is SummaryStep.Highlight -> HighlightDaysCard(
                    highlight = step.highlight,
                    modifier = stepCardModifier
                )

                is SummaryStep.Category -> CategoryDistributionCard(
                    entry = step.entry,
                    total = step.total,
                    modifier = stepCardModifier
                )

                is SummaryStep.Insights -> InsightsStepCard(
                    insights = step.insights,
                    modifier = stepCardModifier
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun InsightsStepCard(
    insights: List<InsightRuleResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Reglas que detectamos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (insights.isEmpty()) {
                Text(
                    text = "Aún no hemos detectado patrones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                insights.take(3).forEach { insight ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = insight.tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = insight.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
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
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSummaryAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onActionClick,
                enabled = !isLoading
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ToneDistributionCard(
    calm: Float,
    impulsive: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Calmadas vs. Impulsivas", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.small)
                    .padding(vertical = 2.dp)
            ) {
                val denom = (calm + impulsive).coerceAtLeast(1f)
                RowBarSegment(
                    calmFraction = calm / denom,
                    calmColor = Color(0xFF69C7A1),
                    impulsiveColor = Color(0xFFFF9D6C)
                )
            }
            Text(
                text = "${calm.toInt()}% calmadas · ${impulsive.toInt()}% impulsivas",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RowBarSegment(
    calmFraction: Float,
    calmColor: Color,
    impulsiveColor: Color
) {
    val safeCalm = calmFraction.coerceIn(0f, 1f)
    val safeImpulsive = (1f - safeCalm).coerceIn(0f, 1f)
    Row(modifier = Modifier.fillMaxSize()) {
        if (safeCalm > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(safeCalm)
                    .background(calmColor)
            )
        }
        if (safeImpulsive > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(safeImpulsive)
                    .background(impulsiveColor)
            )
        }
    }
}

@Composable
private fun HighlightDaysCard(
    highlight: com.dmood.app.domain.usecase.WeeklyHighlight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Días destacados", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Día más positivo: ${highlight.strongestPositiveDay ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Día más retador: ${highlight.strongestNegativeDay ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            highlight.mostFrequentCategory?.let {
                Text(
                    text = "Área más presente: ${it.displayName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CategoryDistributionCard(
    entry: Map.Entry<CategoryType, Int>,
    total: Int,
    modifier: Modifier = Modifier
) {
    if (total == 0) return
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Áreas presentes", style = MaterialTheme.typography.titleMedium)
            Text(text = entry.key.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${((entry.value / total.toFloat()) * 100).toInt()}% de tus decisiones",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
