package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklySummaryViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSummaryContent by rememberSaveable(uiState.summary, uiState.isSummaryAvailable) {
        mutableStateOf(false)
    }
    var showDemo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadWeeklySummary()
    }

    LaunchedEffect(uiState.isSummaryAvailable) {
        if (!uiState.isSummaryAvailable) {
            showSummaryContent = false
        }
    }

    val locale = remember { Locale("es", "ES") }
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM", locale) }

    val weekRange = uiState.summary?.let { summary ->
        val start = Instant.ofEpochMilli(summary.startDate).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(summary.endDate).atZone(zone).toLocalDate()
        "Del ${start.format(dateFormatter)} al ${end.format(dateFormatter)}"
    }
    val nextSummaryFriendly = uiState.nextSummaryDate?.format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
    )?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    val demoData = remember(locale, zone) { buildDemoSummary(locale, zone) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                showLogo = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryHeaderCard(
                    title = uiState.userName?.let { "Tu semana, $it" } ?: "Tu semana",
                    weekRange = weekRange,
                    nextSummaryFriendly = nextSummaryFriendly,
                    isSummaryAvailable = uiState.isSummaryAvailable,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onStartSummary = {
                        showDemo = false
                        showSummaryContent = true
                        viewModel.loadWeeklySummary()
                    },
                    onStartDemo = {
                        showDemo = true
                        showSummaryContent = false
                    }
                )

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.isSummaryAvailable && uiState.summary != null && showSummaryContent -> {
                        SummaryContent(
                            weekRange = weekRange,
                            summary = uiState.summary,
                            highlight = uiState.highlight,
                            insights = uiState.insights,
                            isDemo = false
                        )
                    }

                    showDemo -> {
                        SummaryContent(
                            weekRange = demoData.rangeLabel,
                            summary = demoData.summary,
                            highlight = demoData.highlight,
                            insights = demoData.insights,
                            isDemo = true
                        )
                    }

                    uiState.errorMessage != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeaderCard(
    title: String,
    weekRange: String?,
    nextSummaryFriendly: String?,
    isSummaryAvailable: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onStartSummary: () -> Unit,
    onStartDemo: () -> Unit
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            weekRange?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSummaryAvailable) {
                Text(
                    text = "Tu resumen de la semana correspondiente está listo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onStartSummary,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Empezar")
                }
            } else {
                Text(
                    text = "Aún no tienes resumen semanal, aquí te dejo una demostración para que veas lo que te espera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (nextSummaryFriendly != null) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Próximo resumen: $nextSummaryFriendly") }
                    )
                }
                Button(
                    onClick = onStartDemo,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Empezar Demo")
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(
    weekRange: String?,
    summary: WeeklySummary,
    highlight: WeeklyHighlight?,
    insights: List<InsightRuleResult>,
    isDemo: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekRange?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (isDemo) "Contenido de demostración" else "Resumen generado con tus decisiones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(
                    title = "Tono de la semana",
                    description = highlight?.emotionalTrend ?: "",
                    accentColors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
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

        if (insights.isNotEmpty()) {
            InsightsWrap(insights = insights)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ToneDistributionCard(
    calm: Float,
    impulsive: Float
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(220.dp),
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                RowBarSegment(
                    calmFraction = calm / (calm + impulsive).coerceAtLeast(1f),
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
private fun HighlightDaysCard(highlight: WeeklyHighlight?) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Días destacados", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Día más positivo: ${highlight?.strongestPositiveDay ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Día más retador: ${highlight?.strongestNegativeDay ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )
            highlight?.mostFrequentCategory?.let {
                Text(
                    text = "Área más presente: ${it.displayName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CategoryDistributionCard(entry: Map.Entry<CategoryType, Int>, total: Int) {
    if (total == 0) return
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(220.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsWrap(insights: List<InsightRuleResult>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Reglas que detectamos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            insights.forEach { insight ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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

private data class DemoSummaryData(
    val summary: WeeklySummary,
    val highlight: WeeklyHighlight,
    val insights: List<InsightRuleResult>,
    val rangeLabel: String
)

private fun buildDemoSummary(locale: Locale, zone: ZoneId): DemoSummaryData {
    val end = Instant.now().atZone(zone).toLocalDate().minusDays(1)
    val start = end.minusDays(6)
    val summary = WeeklySummary(
        startDate = start.atStartOfDay(zone).toInstant().toEpochMilli(),
        endDate = end.atStartOfDay(zone).toInstant().toEpochMilli(),
        totalDecisions = 8,
        calmPercentage = 62f,
        impulsivePercentage = 25f,
        neutralPercentage = 13f,
        dailyMoods = mapOf(
            "Lunes" to com.dmood.app.domain.usecase.DailyMood.POSITIVO,
            "Miércoles" to com.dmood.app.domain.usecase.DailyMood.EQUILIBRADO,
            "Viernes" to com.dmood.app.domain.usecase.DailyMood.NEGATIVO
        ),
        categoryDistribution = mapOf(
            CategoryType.SALUD_BIENESTAR to 3,
            CategoryType.TRABAJO_ESTUDIOS to 2,
            CategoryType.RELACIONES_SOCIAL to 3
        )
    )

    val highlight = WeeklyHighlight(
        strongestPositiveDay = "Jueves",
        strongestNegativeDay = "Viernes",
        mostFrequentCategory = CategoryType.SALUD_BIENESTAR,
        emotionalTrend = "Semana predominantemente positiva"
    )

    val insights = listOf(
        InsightRuleResult(
            title = "Fortaleza serena",
            description = "La mayoría de tus decisiones se han tomado con calma.",
            tag = "Ejemplo"
        ),
        InsightRuleResult(
            title = "Área de bienestar al mando",
            description = "Salud y bienestar concentran tu energía esta semana.",
            tag = "Salud"
        )
    )

    val formatter = DateTimeFormatter.ofPattern("dd MMM", locale)
    val label = "Del ${start.format(formatter)} al ${end.format(formatter)}"

    return DemoSummaryData(summary, highlight, insights, label)
}
