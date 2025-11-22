
package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import com.dmood.app.domain.usecase.InsightRuleResult
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

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                showLogo = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        val statusTitle = if (uiState.isSummaryAvailable || uiState.isDemoMode) {
            "El resumen de la semana está listo"
        } else {
            "Aún no tienes resumen semanal"
        }
        val statusDescription = when {
            uiState.isSummaryAvailable -> "El resumen de la semana correspondiente está listo para que lo revises."
            uiState.isDemoMode -> "Estás viendo una demostración de cómo se verá tu resumen semanal."
            else -> "Aun no tienes resumen semanal, aqui te dejo una demostracion para que veas lo que te espera."
        }
        val actionLabel = if (uiState.isSummaryAvailable) "Empezar" else "Empezar Demo"
        val onActionClick = if (uiState.isSummaryAvailable) viewModel::loadWeeklySummary else viewModel::showDemoSummary

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryStatusCard(
                title = statusTitle,
                description = statusDescription,
                userName = uiState.userName,
                weekRange = weekRange,
                nextSummaryFriendly = nextSummaryFriendly,
                isDemo = uiState.isDemoMode,
                actionLabel = actionLabel,
                onActionClick = onActionClick
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.summary == null -> {
                    Text(
                        text = uiState.errorMessage
                            ?: "Registra tus decisiones durante la semana para desbloquear tu primer resumen.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    if (uiState.isDemoMode) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("Modo demo") }
                        )
                    }
                    uiState.summary?.let { summary ->
                        uiState.highlight?.let { highlight ->
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

                            if (uiState.insights.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                InsightsWrap(insights = uiState.insights)
                            }
                        } ?: run {
                            Text(
                                text = "Todavía no hay suficientes decisiones esta semana para generar un resumen. Registra tus decisiones durante varios días y vuelve aquí.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } ?: run {
                        Text(
                            text = "Todavía no hay suficientes decisiones esta semana para generar un resumen. Registra tus decisiones durante varios días y vuelve aquí.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatusCard(
    title: String,
    description: String,
    userName: String?,
    weekRange: String?,
    nextSummaryFriendly: String?,
    isDemo: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = userName?.let { "Hola, $it" } ?: "Hola",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            weekRange?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            nextSummaryFriendly?.let {
                Text(
                    text = "Próximo resumen: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionLabel)
                }
                if (isDemo) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Vista demo") }
                    )
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
