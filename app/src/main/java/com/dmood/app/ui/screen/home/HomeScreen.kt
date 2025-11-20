package com.dmood.app.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import com.dmood.app.ui.theme.toUiColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAddDecisionClick: () -> Unit,
    onOpenSummaryClick: () -> Unit,
    onDecisionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    val locale = remember { Locale("es", "ES") }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, dd/MM", locale) }
    val summaryDateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
    }

    val today = LocalDate.now()
    val isToday = uiState.selectedDate == today
    val canGoBack = uiState.selectedDate.isAfter(uiState.minAvailableDate)

    val formattedDate = remember(uiState.selectedDate) {
        uiState.selectedDate.format(dayFormatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    // Próximo domingo como fecha del resumen semanal
    val nextSummaryDate = remember(today) {
        val dow = today.dayOfWeek
        val daysUntilSunday =
            (DayOfWeek.SUNDAY.value - dow.value + 7) % 7 // 0 si es hoy, >0 si falta
        if (daysUntilSunday == 0) today else today.plusDays(daysUntilSunday.toLong())
    }
    val daysUntilSummary = remember(today, nextSummaryDate) {
        ChronoUnit.DAYS.between(today, nextSummaryDate).toInt()
    }
    val formattedSummaryDate = remember(nextSummaryDate) {
        nextSummaryDate.format(summaryDateFormatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredDecisions = remember(
        uiState.decisions,
        uiState.categoryFilter
    ) {
        uiState.decisions.filter { decision ->
            val matchesCategory = uiState.categoryFilter?.let { decision.category == it } ?: true
            matchesCategory
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                isDeleteMode = uiState.isDeleteMode,
                selectedCount = uiState.selectedForDeletion.size,
                onToggleSelectionMode = viewModel::toggleDeleteMode,
                onDeleteSelectedClick = { showDeleteDialog = true },
                categoryFilter = uiState.categoryFilter,
                onCategorySelected = viewModel::updateCategoryFilter,
                cardLayoutMode = uiState.cardLayout,
                onCardLayoutChange = viewModel::updateCardLayout
            )
        }
        // sin FAB flotante, el botón se integra en el contenido
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp,    // ← FIX REAL
                    start = 0.dp,
                    end = 0.dp
                )
        ) {
            // Fondo degradado suave
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
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // FECHA FIJA ARRIBA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    DateNavigatorRow(
                        formattedDate = formattedDate,
                        isToday = isToday,
                        canGoBack = canGoBack,
                        canGoForward = !isToday,
                        onPrevious = viewModel::goToPreviousDay,
                        onNext = viewModel::goToNextDay
                    )
                }

                // CONTENIDO SCROLL: saludo, filtros, card de decisiones
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            item {
                                HomeHeroCarousel(
                                    userName = uiState.userName,
                                    formattedSummaryDate = formattedSummaryDate,
                                    daysUntilSummary = daysUntilSummary,
                                    onOpenSummaryClick = onOpenSummaryClick,
                                    onAddDecisionClick = onAddDecisionClick,
                                    decisionCount = filteredDecisions.size,
                                    totalDecisions = uiState.decisions.size,
                                    isToday = isToday
                                )
                            }

                            // Filtros (también dentro del scroll)
                            val hasFilters = uiState.categoryFilter != null
                            if (hasFilters) {
                                item {
                                    ActiveFiltersRow(
                                        categoryFilter = uiState.categoryFilter,
                                        onClearCategory = { viewModel.updateCategoryFilter(null) }
                                    )
                                }
                            }

                            // Card contenedora de decisiones y botón "+"
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {

                                        // Botón "+" integrado en la card
                                        if (isToday && !uiState.isDeleteMode) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .clickable { onAddDecisionClick() },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+",
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // Contenido: vacío / filtrado / lista de decisiones
                                        if (filteredDecisions.isEmpty()) {
                                            if (uiState.decisions.isEmpty()) {
                                                EmptyDayCard(isToday = isToday)
                                            } else {
                                                FilteredEmptyState(
                                                    onClearFilters = {
                                                        viewModel.updateCategoryFilter(null)
                                                    }
                                                )
                                            }
                                        } else {
                                            filteredDecisions.forEach { decision ->
                                                val isSelected =
                                                    uiState.selectedForDeletion.contains(decision.id)
                                                val isReadOnly = !isToday

                                                DecisionCard(
                                                    decision = decision,
                                                    isDeleteMode = uiState.isDeleteMode,
                                                    isSelected = isSelected,
                                                    isReadOnly = isReadOnly,
                                                    layoutMode = uiState.cardLayout,
                                                    onCardClick = { onDecisionClick(decision.id) },
                                                    onToggleSelection = {
                                                        viewModel.toggleDecisionSelection(decision.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && uiState.selectedForDeletion.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar decisiones") },
            text = {
                Text(
                    "¿Seguro que quieres borrar ${uiState.selectedForDeletion.size} " +
                            "decisión(es)? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSelectedDecisions()
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ---------- CABECERA (FECHA / SALUDO / RESUMEN) ----------

@Composable
private fun DateNavigatorRow(
    formattedDate: String,
    isToday: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoBack
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Día anterior",
                tint = if (canGoBack)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isToday) "Hoy" else "Histórico",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onNext,
            enabled = canGoForward
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Día siguiente",
                tint = if (canGoForward)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHeroCarousel(
    userName: String?,
    formattedSummaryDate: String,
    daysUntilSummary: Int,
    onOpenSummaryClick: () -> Unit,
    onAddDecisionClick: () -> Unit,
    decisionCount: Int,
    totalDecisions: Int,
    isToday: Boolean
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val flingBehavior = PagerDefaults.flingBehavior(state = pagerState)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(
            state = pagerState,
            flingBehavior = flingBehavior,
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) { page ->
            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                .absoluteValue
            val scale = lerp(0.9f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

            Card(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                val gradientColors = when (page) {
                    0 -> listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                    1 -> listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    else -> listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(gradientColors))
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    when (page) {
                        0 -> GreetingHeroContent(
                            userName = userName,
                            formattedSummaryDate = formattedSummaryDate,
                            daysUntilSummary = daysUntilSummary,
                            onOpenSummaryClick = onOpenSummaryClick
                        )

                        1 -> DecisionOverviewSlide(
                            decisionCount = decisionCount,
                            totalDecisions = totalDecisions,
                            isToday = isToday
                        )

                        2 -> AddDecisionSlide(
                            onAddDecisionClick = onAddDecisionClick,
                            isEnabled = isToday
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(if (selected) 22.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

@Composable
private fun GreetingHeroContent(
    userName: String?,
    formattedSummaryDate: String,
    daysUntilSummary: Int,
    onOpenSummaryClick: () -> Unit
) {
    val displayName = userName?.takeIf { it.isNotBlank() } ?: "de nuevo"

    val summaryStatusText = when {
        daysUntilSummary <= 0 -> "Tu resumen semanal está disponible hoy."
        daysUntilSummary == 1 -> "Tu resumen semanal estará disponible mañana."
        else -> "Tu resumen semanal estará disponible en $daysUntilSummary días."
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Hola, $displayName",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Organiza tu mundo emocional hoy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Resumen semanal",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Próximo resumen: $formattedSummaryDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summaryStatusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Cuantas más decisiones registres esta semana, más completo y útil será tu resumen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onOpenSummaryClick,
                enabled = daysUntilSummary <= 0
            ) {
                Text("Ver resumen semanal")
            }
        }
    }
}

@Composable
private fun DecisionOverviewSlide(
    decisionCount: Int,
    totalDecisions: Int,
    isToday: Boolean
) {
    val subtitle = if (isToday) "Decisiones registradas hoy" else "Decisiones de este día"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$decisionCount en esta vista",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (totalDecisions == 0) "Aún no has registrado decisiones." else "${totalDecisions} en total durante la jornada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillStat(
                label = "Activas",
                value = decisionCount,
                accent = MaterialTheme.colorScheme.primary
            )
            PillStat(
                label = "Jornada",
                value = totalDecisions,
                accent = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun PillStat(label: String, value: Int, accent: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.large)
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AddDecisionSlide(onAddDecisionClick: () -> Unit, isEnabled: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isEnabled) "Añade una nueva decisión" else "Solo lectura",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isEnabled) "Captura lo que sientes ahora mismo." else "Solo puedes consultar las decisiones de ese día.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onAddDecisionClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registrar decisión")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFiltersRow(
    categoryFilter: CategoryType?,
    onClearCategory: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (categoryFilter != null) {
            AssistChip(
                onClick = onClearCategory,
                label = { Text(categoryFilter.displayName) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(categoryFilter.toUiColor())
                    )
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun FilteredEmptyState(onClearFilters: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sin coincidencias",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "No encontramos decisiones para los filtros aplicados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onClearFilters) {
                Text("Limpiar filtros")
            }
        }
    }
}

@Composable
private fun EmptyDayCard(isToday: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isToday)
                    "Hoy aún no has registrado ninguna decisión"
                else
                    "No hay decisiones registradas en este día",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isToday)
                    "Añade una cuando ocurra algo relevante para ti. Tu futuro yo te lo agradecerá."
                else
                    "Puedes seguir explorando otros días o volver a hoy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- TOPBAR ----------

@Composable
private fun HomeTopBar(
    isDeleteMode: Boolean,
    selectedCount: Int,
    onToggleSelectionMode: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    categoryFilter: CategoryType?,
    onCategorySelected: (CategoryType?) -> Unit,
    cardLayoutMode: CardLayoutMode,
    onCardLayoutChange: (CardLayoutMode) -> Unit
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var layoutMenuExpanded by remember { mutableStateOf(false) }

    val subtitle = if (isDeleteMode) {
        if (selectedCount > 0) "${selectedCount} seleccionada(s)"
        else "Toca las tarjetas para elegir"
    } else {
        null
    }

    DmoodTopBar(
        title = "D-Mood",
        subtitle = subtitle,
        actions = {
            if (isDeleteMode) {
                IconButton(onClick = onToggleSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar modo selección"
                    )
                }
                IconButton(
                    onClick = onDeleteSelectedClick,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar decisiones"
                    )
                }
            } else {
                IconButton(onClick = { filterMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filtrar decisiones",
                        tint = if (categoryFilter != null)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas las categorías") },
                        onClick = {
                            filterMenuExpanded = false
                            onCategorySelected(null)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.FilterList, contentDescription = null)
                        },
                        trailingIcon = {
                            if (categoryFilter == null) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    CategoryType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                filterMenuExpanded = false
                                onCategorySelected(type)
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(type.toUiColor())
                                )
                            },
                            trailingIcon = {
                                if (categoryFilter == type) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }

                IconButton(onClick = { layoutMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.ViewAgenda,
                        contentDescription = "Opciones de vista"
                    )
                }
                DropdownMenu(
                    expanded = layoutMenuExpanded,
                    onDismissRequest = { layoutMenuExpanded = false }
                ) {
                    listOf(CardLayoutMode.COMPACT, CardLayoutMode.COZY, CardLayoutMode.ROOMY)
                        .forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            mode.label,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    layoutMenuExpanded = false
                                    onCardLayoutChange(mode)
                                },
                                trailingIcon = {
                                    if (cardLayoutMode == mode) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                }

                IconButton(onClick = onToggleSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Seleccionar para borrar"
                    )
                }
            }
        },
        showLogo = true
    )
}

// ---------- CARDS DE DECISIÓN ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionCard(
    decision: Decision,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    isReadOnly: Boolean,
    layoutMode: CardLayoutMode,
    onCardClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val intensityColor = when (decision.intensity) {
        1 -> Color(0xFFC8E6C9)
        2 -> Color(0xFFA5D6A7)
        3 -> Color(0xFF81C784)
        4 -> Color(0xFF4CAF50)
        5 -> Color(0xFF2E7D32)
        else -> Color(0xFFC8E6C9)
    }

    val metrics = remember(layoutMode) { layoutMode.metrics() }

    val enabledClick = isDeleteMode || !isReadOnly
    val clickAction: () -> Unit = when {
        isDeleteMode -> onToggleSelection
        isReadOnly -> ({})
        else -> onCardClick
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabledClick,
                onClick = clickAction
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected && isDeleteMode ->
                    MaterialTheme.colorScheme.surfaceVariant
                isReadOnly ->
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                else ->
                    MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = metrics.horizontalPadding,
                    vertical = metrics.verticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(metrics.verticalSpacing)
        ) {
            if (isDeleteMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Text(
                text = decision.text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = metrics.titleLines,
                overflow = TextOverflow.Ellipsis
            )

            if (layoutMode != CardLayoutMode.COMPACT) {
                CategoryTag(category = decision.category)
            }

            if (layoutMode == CardLayoutMode.ROOMY) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    decision.emotions.take(2).forEach { emotion ->
                        val background = emotion.color
                        val textColor =
                            if (background.luminance() > 0.6f) Color.Black else Color.White
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(emotion.displayName) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = background.copy(alpha = 0.85f),
                                disabledLabelColor = textColor
                            )
                        )
                    }

                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Intensidad ${decision.intensity}/5") },
                        leadingIcon = {
                            val icon =
                                if (decision.intensity >= 3) Icons.Filled.ArrowCircleUp
                                else Icons.Filled.ArrowCircleDown
                            Icon(icon, contentDescription = null)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = intensityColor,
                            disabledLabelColor = Color.Black
                        )
                    )

                    if (isReadOnly) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("Solo lectura") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTag(category: CategoryType) {
    val background = category.toUiColor()
    val textColor = if (background.luminance() > 0.6f) Color.Black else Color.White
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(category.displayName) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = background,
            disabledLabelColor = textColor
        )
    )
}

private data class DecisionCardMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val verticalSpacing: Dp,
    val titleLines: Int
)

private fun CardLayoutMode.metrics(): DecisionCardMetrics = when (this) {
    CardLayoutMode.COMPACT -> DecisionCardMetrics(
        horizontalPadding = 16.dp,
        verticalPadding = 12.dp,
        verticalSpacing = 8.dp,
        titleLines = 2
    )

    CardLayoutMode.COZY -> DecisionCardMetrics(
        horizontalPadding = 20.dp,
        verticalPadding = 16.dp,
        verticalSpacing = 10.dp,
        titleLines = 3
    )

    CardLayoutMode.ROOMY -> DecisionCardMetrics(
        horizontalPadding = 24.dp,
        verticalPadding = 20.dp,
        verticalSpacing = 12.dp,
        titleLines = 4
    )
}
