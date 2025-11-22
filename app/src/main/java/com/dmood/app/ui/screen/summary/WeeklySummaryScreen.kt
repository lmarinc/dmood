package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.ui.components.DmoodTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

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

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val gradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface
            )
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                showLogo = true
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryIntroCard(
                    userName = uiState.userName,
                    weekRange = weekRange,
                    isSummaryAvailable = uiState.isSummaryAvailable || uiState.isDemo,
                    nextSummaryFriendly = nextSummaryFriendly,
                    isDemo = uiState.isDemo,
                    onStart = {
                        scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                    },
                    onStartDemo = viewModel::loadDemoSummary
                )

                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.summary != null && uiState.highlight != null -> {
                        SummaryContent(
                            summary = uiState.summary,
                            highlight = uiState.highlight,
                            insights = uiState.insights
                        )
                    }

                    uiState.summary != null -> {
                        Text(
                            text = "Todavía no hay suficientes decisiones esta semana para generar un resumen. Registra tus decisiones durante varios días y vuelve aquí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        SummaryEmptyCard(
                            nextSummaryFriendly = nextSummaryFriendly,
                            errorMessage = uiState.errorMessage,
                            onStartDemo = viewModel::loadDemoSummary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryIntroCard(
    userName: String?,
    weekRange: String?,
    isSummaryAvailable: Boolean,
    nextSummaryFriendly: String?,
    isDemo: Boolean,
    onStart: () -> Unit,
    onStartDemo: () -> Unit
) {
    SummarySurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = userName?.let { "Tu semana, $it" } ?: "Tu semana",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            weekRange?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val message = when {
                isDemo -> "Estás viendo una demostración. Así se verá tu resumen semanal real."
                isSummaryAvailable -> "El resumen de la semana correspondiente está listo."
                else -> "Aún no tienes resumen semanal, aquí te dejo una demostración para que veas lo que te espera."
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = if (isSummaryAvailable) onStart else onStartDemo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSummaryAvailable) "Empezar" else "Empezar Demo")
                }
                if (nextSummaryFriendly != null && !isSummaryAvailable) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Próximo resumen: $nextSummaryFriendly") }
                    )
                }
                if (isDemo) {
                    AssistChip(onClick = {}, enabled = false, label = { Text("Demo") })
                }
            }
        }
    }
}

@Composable
private fun SummaryEmptyCard(
    nextSummaryFriendly: String?,
    errorMessage: String?,
    onStartDemo: () -> Unit
) {
    SummarySurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Aún no tienes resumen semanal, aquí tienes una demostración de lo que te espera.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            nextSummaryFriendly?.let {
                Text(
                    text = "Próximo resumen: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = onStartDemo) {
                Text("Empezar Demo")
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summary: com.dmood.app.domain.usecase.WeeklySummary,
    highlight: com.dmood.app.domain.usecase.WeeklyHighlight,
    insights: List<InsightRuleResult>
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
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        }
        item {
            ToneDistributionCard(
                calm = summary.calmPercentage,
                impulsive = summary.impulsivePercentage
            )
        }
        item { HighlightDaysCard(highlight = highlight) }
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
        Spacer(modifier = Modifier.height(8.dp))
        InsightsWrap(insights = insights)
    }
}

@Composable
private fun SummarySurfaceCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
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
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(text = description, style = MaterialTheme.typography.bodyLarge, color = Color.White)
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
private fun HighlightDaysCard(highlight: com.dmood.app.domain.usecase.WeeklyHighlight) {
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
                text = "${entry.value} decisiones (${((entry.value.toFloat() / total.toFloat()) * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InsightsWrap(insights: List<InsightRuleResult>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        insights.forEach { insight ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.width(260.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = insight.tag.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = insight.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = insight.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
