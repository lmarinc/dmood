package com.dmood.app.ui.screen.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import com.dmood.app.ui.screen.home.CardLayoutMode.COMPACT
import com.dmood.app.ui.screen.home.CardLayoutMode.COZY
import com.dmood.app.ui.screen.home.CardLayoutMode.ROOMY
import com.dmood.app.ui.theme.toUiColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAddDecisionClick: () -> Unit,
    onOpenSummaryClick: () -> Unit, // mantengo la firma, aunque aquí no se use
    onDecisionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    val locale = remember { Locale("es", "ES") }
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, dd/MM", locale) }
    val formattedDate = remember(uiState.selectedDate) {
        uiState.selectedDate.format(formatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }
    val isToday = uiState.selectedDate == LocalDate.now()

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Filtrado solo por categoría (sin búsqueda de texto)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDecisionClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Nueva decisión",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fondo suave con ligero gradiente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // HEADER DE SALUDO
                item {
                    HomeGreetingHeader(
                        userName = uiState.userName
                    )
                }

                // Selector de día
                item {
                    DaySelectorCard(
                        formattedDate = formattedDate,
                        isToday = isToday,
                        onPrevious = viewModel::goToPreviousDay,
                        onNext = viewModel::goToNextDay
                    )
                }

                // Filtros activos (solo categoría)
                val hasFilters = uiState.categoryFilter != null
                if (hasFilters) {
                    item {
                        ActiveFiltersRow(
                            categoryFilter = uiState.categoryFilter,
                            onClearCategory = { viewModel.updateCategoryFilter(null) }
                        )
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    if (filteredDecisions.isEmpty()) {
                        item {
                            if (uiState.decisions.isEmpty()) {
                                EmptyDayCard()
                            } else {
                                FilteredEmptyState(
                                    onClearFilters = {
                                        viewModel.updateCategoryFilter(null)
                                    }
                                )
                            }
                        }
                    } else {
                        items(filteredDecisions, key = { it.id }) { decision ->
                            val isSelected = uiState.selectedForDeletion.contains(decision.id)
                            DecisionCard(
                                decision = decision,
                                isDeleteMode = uiState.isDeleteMode,
                                isSelected = isSelected,
                                layoutMode = uiState.cardLayout,
                                onCardClick = { onDecisionClick(decision.id) },
                                onToggleSelection = { viewModel.toggleDecisionSelection(decision.id) }
                            )
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

@Composable
private fun HomeGreetingHeader(
    userName: String?
) {
    val displayName = userName?.takeIf { it.isNotBlank() } ?: "de nuevo"
    val initial = userName?.firstOrNull()?.uppercaseChar()?.toString() ?: "D"

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Hola $displayName",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Registra, reflexiona y mejora cada día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Apunta tus decisiones importantes y aprende de ellas cada semana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

//            Box(
//                modifier = Modifier
//                    .size(44.dp)
//                    .clip(CircleShape)
//                    .background(
//                        Brush.radialGradient(
//                            colors = listOf(
//                                MaterialTheme.colorScheme.primary,
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
//                            )
//                        )
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = initial,
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
        }
    }
}

@Composable
private fun DaySelectorCard(
    formattedDate: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Día anterior"
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
                enabled = !isToday
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Día siguiente"
                )
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

@Composable
private fun EmptyDayCard() {
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
                text = "Hoy aún no has registrado ninguna decisión",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Añade una cuando ocurra algo relevante para ti. Tu futuro yo te lo agradecerá.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
        subtitle = subtitle ?: "",
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
                // Filtro por categoría
                IconButton(onClick = { filterMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filtrar decisiones",
                        tint = if (categoryFilter != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
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

                // Selector de tamaño de card (lista / normal / grande)
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
                    listOf(COMPACT, COZY, ROOMY).forEach { mode ->
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

                // Entrada en modo selección desde el TopBar
                IconButton(onClick = onToggleSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Seleccionar para borrar"
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionCard(
    decision: Decision,
    isDeleteMode: Boolean,
    isSelected: Boolean,
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

    val clickAction = if (isDeleteMode) onToggleSelection else onCardClick
    val metrics = remember(layoutMode) { layoutMode.metrics() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = clickAction),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && isDeleteMode) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
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

            // Modo NORMAL / GRANDE muestran categoría
            if (layoutMode != COMPACT) {
                CategoryTag(category = decision.category)
            }

            // Solo en modo GRANDE mostramos emociones + intensidad
            if (layoutMode == ROOMY) {
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

// COMPACT = solo texto
// COZY = texto + categoría
// ROOMY = texto + categoría + emociones + intensidad
private fun CardLayoutMode.metrics(): DecisionCardMetrics = when (this) {
    COMPACT -> DecisionCardMetrics(
        horizontalPadding = 16.dp,
        verticalPadding = 12.dp,
        verticalSpacing = 8.dp,
        titleLines = 2
    )

    COZY -> DecisionCardMetrics(
        horizontalPadding = 20.dp,
        verticalPadding = 16.dp,
        verticalSpacing = 10.dp,
        titleLines = 3
    )

    ROOMY -> DecisionCardMetrics(
        horizontalPadding = 24.dp,
        verticalPadding = 20.dp,
        verticalSpacing = 12.dp,
        titleLines = 4
    )
}
