package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.domain.usecase.DailyMood as WeeklyDailyMood
import com.dmood.app.domain.usecase.InsightRuleResult
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import com.dmood.app.ui.theme.toUiColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val summary = uiState.summary
    val highlight = uiState.highlight
    val weekStartLabel = uiState.weekStartDay.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

    val weekRange = summary?.let { s ->
        val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES"))
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(s.startDate).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(s.endDate).atZone(zone).toLocalDate()
        "Del ${start.format(formatter)} al ${end.format(formatter)}"
    }

    val nextSummaryFriendly = uiState.nextSummaryDate?.format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
    )?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
    }

    val heroInsight = uiState.insights.firstOrNull()

    // Pasos del carrusel
    val pagerSteps = remember(summary, highlight, uiState.insights, heroInsight, weekRange) {
        buildList<SummaryPagerStep> {
            if (summary != null && highlight != null && uiState.isSummaryAvailable) {
                add(
                    SummaryPagerStep.Hero(
                        weekRange = weekRange,
                        title = heroInsight?.title ?: "Tono general",
                        tag = heroInsight?.tag ?: "Resumen",
                        description = heroInsight?.description ?: highlight.emotionalTrend
                    )
                )
                add(
                    SummaryPagerStep.Categories(
                        categoryDistribution = summary.categoryDistribution,
                        totalDecisions = summary.totalDecisions
                    )
                )
                add(
                    SummaryPagerStep.Days(
                        dailyMoods = summary.dailyMoods
                    )
                )
                if (uiState.insights.isNotEmpty()) {
                    add(
                        SummaryPagerStep.Insights(
                            insights = uiState.insights
                        )
                    )
                }
            }
        }
    }

    // Si deja de haber resumen disponible, reseteamos el flujo
    LaunchedEffect(uiState.isSummaryAvailable) {
        if (!uiState.isSummaryAvailable) {
            hasStarted = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Resumen semanal",
                subtitle = "Tu semana tipo \"Wrapped\"",
                showLogo = true
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cargando tu resumen semanal...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                uiState.errorMessage != null && summary == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "No hemos podido cargar tu resumen.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    // Vista antes de empezar: cabecera + botón "Ver mi resumen"
                    if (!hasStarted) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                SummaryHeaderCard(
                                    userName = uiState.userName,
                                    weekRange = weekRange,
                                    nextSummaryFriendly = nextSummaryFriendly,
                                    isSummaryAvailable = uiState.isSummaryAvailable,
                                    weekStartLabel = weekStartLabel,
                                    isDemo = uiState.isDemo,
                                    onActionClick = {
                                        viewModel.loadWeeklySummary()
                                    }
                                )
                            }

                            if (summary != null && highlight != null && uiState.isSummaryAvailable && pagerSteps.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(48.dp)) }
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(
                                            onClick = { hasStarted = true },
                                            shape = MaterialTheme.shapes.large,
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 6.dp
                                            ),
                                            contentPadding = PaddingValues(
                                                horizontal = 32.dp,
                                                vertical = 12.dp
                                            )
                                        ) {
                                            Text(
                                                text = "Ver mi resumen",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        text = "Todavía no hay suficientes decisiones registradas para generar un resumen tipo \"Wrapped\". " +
                                                "Cuando tengas varios días con decisiones, esta pantalla se llenará de datos.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else {
                        // Vista después de pulsar "Ver mi resumen": solo carrusel
                        if (summary != null && highlight != null && uiState.isSummaryAvailable && pagerSteps.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                SummaryStepsPager(
                                    steps = pagerSteps,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No hay datos suficientes para mostrar el resumen en modo pasos.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { hasStarted = false }) {
                                    Text("Volver")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------- MODELO DE PASOS DEL CARRUSEL ---------

private sealed class SummaryPagerStep {
    data class Hero(
        val weekRange: String?,
        val title: String,
        val tag: String,
        val description: String
    ) : SummaryPagerStep()

    data class Categories(
        val categoryDistribution: Map<CategoryType, Int>,
        val totalDecisions: Int
    ) : SummaryPagerStep()

    data class Days(
        val dailyMoods: Map<String, WeeklyDailyMood>
    ) : SummaryPagerStep()

    data class Insights(
        val insights: List<InsightRuleResult>
    ) : SummaryPagerStep()
}

// --------- CABECERA / CONTEXTO ---------

@Composable
private fun SummaryHeaderCard(
    userName: String?,
    weekRange: String?,
    nextSummaryFriendly: String?,
    isSummaryAvailable: Boolean,
    weekStartLabel: String?,
    isDemo: Boolean,
    onActionClick: () -> Unit
) {
    val headline = userName?.let { "Así se ve tu semana, $it" } ?: "Así se ve tu semana"
    val subtitle = when {
        isDemo -> "Estás viendo un ejemplo de cómo se verá tu resumen cuando registres decisiones reales."
        isSummaryAvailable -> "Este resumen se construye solo a partir de las decisiones que has registrado esta semana."
        else -> "Cuando registres decisiones durante varios días, aquí verás un resumen tipo \"Wrapped\"."
    }

    val actionLabel = if (isSummaryAvailable) "Actualizar" else "Volver a calcular"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            weekRange?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            weekStartLabel?.let {
                Text(
                    text = "Tu semana empieza el $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            nextSummaryFriendly?.let {
                Text(
                    text = "Próximo resumen estimado: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}

// --------- CARRUSEL HORIZONTAL DE PASOS ---------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummaryStepsPager(
    steps: List<SummaryPagerStep>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { steps.size })

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val step = steps[page]
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                when (step) {
                    is SummaryPagerStep.Hero -> StepCard(
                        stepNumber = page + 1,
                        title = "Tema de tu semana",
                        description = "Foto rápida del tono general de tus decisiones."
                    ) {
                        HeroSummaryCard(
                            weekRange = null,
                            heroTitle = step.title,
                            heroTag = "",
                            heroDescription = step.description
                        )
                    }

                    is SummaryPagerStep.Categories -> StepCard(
                        stepNumber = page + 1,
                        title = "Áreas que más han aparecido",
                        description = "Campos de tu vida donde más has registrado decisiones."
                    ) {
                        CategorySection(
                            categoryDistribution = step.categoryDistribution,
                            total = step.totalDecisions
                        )
                    }

                    is SummaryPagerStep.Days -> StepCard(
                        stepNumber = page + 1,
                        title = "Cómo se ha sentido cada día",
                        description = "Días más ligeros y días más cargados según tus decisiones."
                    ) {
                        DaysOverviewSection(
                            dailyMoods = step.dailyMoods
                        )
                    }

                    is SummaryPagerStep.Insights -> StepCard(
                        stepNumber = page + 1,
                        title = "Patrones de la semana",
                        description = "Frases que describen lo que se repite en tus datos."
                    ) {
                        InsightsWrap(insights = step.insights)
                    }
                }

                // Espacio extra al final de cada página para que la última card no quede cortada
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Indicadores de página (fijos abajo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(steps.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (isSelected) 18.dp else 10.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

// --------- PASO (CABECERA + CONTENIDO) ---------

@Composable
private fun StepCard(
    stepNumber: Int,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Paso $stepNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

// --------- CARDS DE CONTENIDO ---------

@Composable
private fun HeroSummaryCard(
    weekRange: String?,
    heroTitle: String,
    heroTag: String,
    heroDescription: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = heroTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = heroDescription,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CategorySection(
    categoryDistribution: Map<CategoryType, Int>,
    total: Int
) {
    if (total == 0 || categoryDistribution.isEmpty()) {
        Text(
            text = "De momento no hay suficientes decisiones como para ver áreas destacadas.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val sorted = categoryDistribution.entries
        .sortedByDescending { it.value }
        .take(4)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sorted.forEach { entry ->
            CategoryRow(entry = entry, total = total)
        }
    }
}

@Composable
private fun CategoryRow(
    entry: Map.Entry<CategoryType, Int>,
    total: Int
) {
    val percentage = (entry.value.toFloat() / total.toFloat() * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.key.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${entry.value} decisiones · $percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(56.dp) // mismo tamaño para todas las barras
                    .clip(MaterialTheme.shapes.small)
                    .background(entry.key.toUiColor())
            )
        }
    }
}

@Composable
private fun DaysOverviewSection(
    dailyMoods: Map<String, WeeklyDailyMood>
) {
    if (dailyMoods.isEmpty()) {
        Text(
            text = "Aún no hay suficientes días con decisiones registradas como para ver un patrón semanal.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dailyMoods.forEach { (dayName, mood) ->
            DayMoodRow(dayName = dayName, mood = mood)
        }
    }
}

@Composable
private fun DayMoodRow(
    dayName: String,
    mood: WeeklyDailyMood
) {
    val localizedDay = dayName.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
    }

    val (label, color) = when (mood) {
        WeeklyDailyMood.POSITIVO -> "Día con tono positivo" to Color(0xFF69C7A1)
        WeeklyDailyMood.NEGATIVO -> "Día con tono intenso" to Color(0xFFFF6F6F)
        WeeklyDailyMood.NEUTRO -> "Día de tono neutro" to Color(0xFFB0BEC5)
        WeeklyDailyMood.NORMAL -> "Día equilibrado" to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = localizedDay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(56.dp) // mismo tamaño para todas las barras
                    .clip(MaterialTheme.shapes.small)
                    .background(color)
            )
        }
    }
}

// --------- INSIGHTS CON EMOCIONES SUBRAYADAS ---------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsWrap(
    insights: List<InsightRuleResult>
) {
    if (insights.isEmpty()) return

    val emotionNames = remember {
        EmotionType.values().map { it.displayName }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            insights.forEach { insight ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = insight.tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        val annotated = buildAnnotatedString {
                            append(insight.description)
                            emotionNames.forEach { name ->
                                var startIndex = insight.description.indexOf(name)
                                while (startIndex >= 0) {
                                    addStyle(
                                        style = SpanStyle(
                                            textDecoration = TextDecoration.Underline,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        start = startIndex,
                                        end = startIndex + name.length
                                    )
                                    startIndex = insight.description.indexOf(
                                        name,
                                        startIndex + name.length
                                    )
                                }
                            }
                        }

                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
