package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklySummary
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklySummaryViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showContent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadWeeklySummary()
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

    val canShowSummary = uiState.summary != null && uiState.highlight != null

    LaunchedEffect(canShowSummary) {
        if (!canShowSummary) {
            showContent = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                showLogo = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryStatusCard(
                    title = if (canShowSummary) "Tu resumen está listo" else "Aún no tienes resumen semanal",
                    description = if (canShowSummary) {
                        "El resumen de la semana ${weekRange ?: ""} está preparado para ti."
                    } else {
                        "Aun no tienes resumen semanal, aqui te dejo una demostracion para que veas lo que te espera."
                    },
                    secondaryInfo = nextSummaryFriendly?.let { "Próximo resumen: $it" },
                    actionLabel = if (canShowSummary) "Empezar" else "Empezar Demo",
                    onAction = {
                        showContent = true
                        if (!canShowSummary) viewModel.startDemoPreview()
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                !canShowSummary && uiState.errorMessage != null -> {
                    item {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (showContent && canShowSummary) {
                uiState.summary?.let { summary ->
                    uiState.highlight?.let { highlight ->
                        item {
                            SummaryHighlightsRow(
                                summary = summary,
                                highlight = highlight
                            )
                        }
                        if (uiState.insights.isNotEmpty()) {
                            item {
                                InsightsWrap(insights = uiState.insights)
                            }
                        }
                    }
                }
            } else if (!uiState.isLoading && !canShowSummary) {
                item {
                    Text(
                        text = "Cuando tengas suficientes decisiones, tu resumen semanal aparecerá aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatusCard(
    title: String,
    description: String,
    secondaryInfo: String?,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (secondaryInfo != null) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(secondaryInfo) }
                )
            }
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SummaryHighlightsRow(
    summary: WeeklySummary,
    highlight: com.dmood.app.domain.usecase.WeeklyHighlight
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryCard(
                title = "Tono de la semana",
                description = highlight.emotionalTrend,
                accentColors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                )
            )
        }
        item {
            ToneDistributionCard(
                calm = summary.calmPercentage,
                impulsive = summary.impulsivePercentage
            )
        }
        item {
            HighlightDaysCard(highlight = highlight)
        }
        items(
            summary.categoryDistribution.entries
                .sortedByDescending { it.value }
                .take(3)
        ) { entry ->
            CategoryDistributionCard(
                entry = entry,
                total = summary.totalDecisions
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    description: String,
    accentColors: List<Color>
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = accentColors)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToneDistributionCard(calm: Float, impulsive: Float) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Balance emocional",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Calmada vs Impulsiva",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${calm.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Calmadas",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${impulsive.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Impulsivas",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightDaysCard(highlight: com.dmood.app.domain.usecase.WeeklyHighlight) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Días destacados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HighlightRow(label = "Más luminoso", value = highlight.strongestPositiveDay)
            HighlightRow(label = "Más retador", value = highlight.strongestNegativeDay)
            HighlightRow(
                label = "Área protagonista",
                value = highlight.mostFrequentCategory?.displayName
            )
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = value ?: "Sin datos suficientes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryDistributionCard(
    entry: Map.Entry<CategoryType, Int>,
    total: Int
) {
    val ratio = if (total == 0) 0f else entry.value.toFloat() / total.toFloat()
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = entry.key.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(ratio * 100).toInt()}% de tus decisiones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((ratio * 180).dp)
                        .background(
                            color = entry.key.colorForCategory(),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

@Composable
private fun CategoryType.colorForCategory(): Color {
    return when (this) {
        CategoryType.RELACIONES -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        CategoryType.SALUD -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        CategoryType.PROYECTOS -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        CategoryType.BIENESTAR -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        CategoryType.OTROS -> MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun InsightsWrap(insights: List<InsightRuleResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pistas rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                insights.forEach { insight ->
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            label = { Text(insight.tag) }
                        )
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = insight.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
