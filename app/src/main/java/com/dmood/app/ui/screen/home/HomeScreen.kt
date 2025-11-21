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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import com.dmood.app.ui.theme.toUiColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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

    val nextSummaryDate = uiState.nextSummaryDate ?: today
    val daysUntilSummary = remember(today, nextSummaryDate, uiState.summaryAvailableToday) {
        if (uiState.summaryAvailableToday) 0
        else ChronoUnit.DAYS.between(today, nextSummaryDate).toInt().coerceAtLeast(0)
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
                isToday = isToday,
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
                    bottom = 0.dp,
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
                        val hasDecisionsToday = uiState.decisions.isNotEmpty()
                        val pages = remember(hasDecisionsToday) {
                            buildList {
                                add(HomePagerPage.GREETING)
                                if (hasDecisionsToday) add(HomePagerPage.DECISIONS)
                                add(HomePagerPage.ADD)
                            }
                        }
                        val pagerState = rememberPagerState(pageCount = { pages.size })

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { index ->
                                when (pages[index]) {
                                    HomePagerPage.GREETING -> GreetingAndSummaryCard(
                                        userName = uiState.userName,
                                        formattedSummaryDate = formattedSummaryDate,
                                        daysUntilSummary = daysUntilSummary,
                                        summaryAvailable = uiState.summaryAvailableToday,
                                        onOpenSummaryClick = onOpenSummaryClick
                                    )

                                    HomePagerPage.DECISIONS -> DecisionsDeckCard(
                                        isToday = isToday,
                                        decisions = filteredDecisions,
                                        totalDecisions = uiState.decisions.size,
                                        isDeleteMode = uiState.isDeleteMode,
                                        selectedForDeletion = uiState.selectedForDeletion,
                                        layoutMode = uiState.cardLayout,
                                        categoryFilter = uiState.categoryFilter,
                                        onClearFilters = { viewModel.updateCategoryFilter(null) },
                                        onDecisionClick = onDecisionClick,
                                        onToggleSelection = { decisionId ->
                                            viewModel.toggleDecisionSelection(decisionId)
                                        },
                                        onAddDecisionClick = onAddDecisionClick
                                    )

                                    HomePagerPage.ADD -> AddDecisionCard(
                                        isToday = isToday,
                                        hasDecisions = hasDecisionsToday,
                                        onAddDecisionClick = onAddDecisionClick
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                pages.forEachIndexed { pageIndex, page ->
                                    val active = pagerState.currentPage == pageIndex
                                    val width = if (active) 18.dp else 8.dp
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .height(8.dp)
                                            .width(width)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(
                                                if (active) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            )
                                    )
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

private enum class HomePagerPage { GREETING, DECISIONS, ADD }

@Composable
private fun GreetingAndSummaryCard(
    userName: String?,
    formattedSummaryDate: String,
    daysUntilSummary: Int,
    summaryAvailable: Boolean,
    onOpenSummaryClick: () -> Unit
    ) {
        val displayName = userName?.takeIf { it.isNotBlank() } ?: "de nuevo"

    val summaryStatusText = when {
        summaryAvailable -> "Tu resumen semanal está disponible hoy."
        daysUntilSummary == 0 -> "Tu resumen se libera en unas horas."
        daysUntilSummary == 1 -> "Tu resumen semanal estará disponible mañana."
        else -> "Tu resumen semanal estará disponible en $daysUntilSummary días."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hola, $displayName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Organiza tu mundo emocional hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summaryStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cuantas más decisiones registres esta semana, más completo y útil será tu resumen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onOpenSummaryClick,
                        enabled = summaryAvailable
                    ) {
                        Text("Ver resumen semanal")
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionsDeckCard(
    isToday: Boolean,
    decisions: List<Decision>,
    totalDecisions: Int,
    isDeleteMode: Boolean,
    selectedForDeletion: Set<Long>,
    layoutMode: CardLayoutMode,
    categoryFilter: CategoryType?,
    onClearFilters: () -> Unit,
    onDecisionClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onAddDecisionClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val subtitleText = when {
        totalDecisions == 0 && isToday -> "Empieza registrando tu primera decisión de hoy."
        totalDecisions == 0 && !isToday -> "No hay decisiones guardadas en este día."
        isToday -> "$totalDecisions decisiones registradas hoy."
        else -> "$totalDecisions decisiones guardadas."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Tus decisiones",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isToday && !isDeleteMode) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onAddDecisionClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Añadir decisión",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (categoryFilter != null) {
                ActiveFiltersRow(
                    categoryFilter = categoryFilter,
                    onClearCategory = onClearFilters
                )
            }

            when {
                decisions.isEmpty() && totalDecisions == 0 -> {
                    Text(
                        text = if (isToday)
                            "Cuando tomes una decisión importante, regístrala aquí para poder revisarla después."
                        else
                            "No hay decisiones registradas para esta fecha.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                decisions.isEmpty() -> {
                    FilteredEmptyState(onClearFilters = onClearFilters)
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        decisions.forEach { decision ->
                            val isSelected = selectedForDeletion.contains(decision.id)

                            DecisionCard(
                                decision = decision,
                                isDeleteMode = isDeleteMode,
                                isSelected = isSelected,
                                isReadOnly = !isToday,
                                layoutMode = layoutMode,
                                onCardClick = { onDecisionClick(decision.id) },
                                onToggleSelection = { onToggleSelection(decision.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDecisionCard(
    isToday: Boolean,
    hasDecisions: Boolean,
    onAddDecisionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasDecisions) "Añade otra decisión" else "Empieza con tu primera decisión",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = if (hasDecisions)
                    "Cada nuevo registro afina tu mapa emocional y hace más precisas las reglas de la semana."
                else
                    "Captura las decisiones importantes de hoy para desbloquear tu primer resumen semanal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onAddDecisionClick,
                enabled = isToday,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isToday) "Añadir decisión" else "Solo puedes añadir en el día actual")
            }
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

// ---------- TOPBAR ----------

@Composable
private fun HomeTopBar(
    isDeleteMode: Boolean,
    selectedCount: Int,
    isToday: Boolean,
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
            if (isDeleteMode && isToday) {
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

                if (isToday) {
                    IconButton(onClick = onToggleSelectionMode) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Seleccionar para borrar"
                        )
                    }
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

    val enabledClick = !isReadOnly
    val clickAction: () -> Unit = when {
        isDeleteMode -> onToggleSelection
        isReadOnly -> ({})
        else -> onCardClick
    }

    val border = if (isSelected && isDeleteMode) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    val containerColor = when {
        isReadOnly -> MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabledClick,
                onClick = clickAction
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isReadOnly) 2.dp else 8.dp
        ),
        shape = MaterialTheme.shapes.large,
        border = border
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
            // Fila superior: categoría + estado (solo lectura / selección)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (layoutMode != CardLayoutMode.COMPACT) {
                    CategoryTag(category = decision.category)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isReadOnly && !isDeleteMode) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Solo lectura",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isDeleteMode && !isReadOnly) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() }
                        )
                    }
                }
            }

            // Texto principal de la decisión
            Text(
                text = decision.text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = metrics.titleLines,
                overflow = TextOverflow.Ellipsis
            )

            // Emojis / información extra solo en modo ROOMY
            if (layoutMode == CardLayoutMode.ROOMY) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Emociones (máx 2) como chips suaves
                    decision.emotions.take(2).forEach { emotion ->
                        val background = emotion.color
                        val textColor =
                            if (background.luminance() > 0.6f) Color.Black else Color.White
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(emotion.displayName) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = background.copy(alpha = 0.9f),
                                disabledLabelColor = textColor
                            )
                        )
                    }

                    // Intensidad
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
