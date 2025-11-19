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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.Decision
import com.dmood.app.ui.DmoodViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAddDecisionClick: () -> Unit,
    onOpenSummaryClick: () -> Unit, // se mantiene por la firma, pero aquí ya no se usa
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
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
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Saludo + copy
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.userName?.let { "Hola, $it" } ?: "Hola",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Organiza tu mundo emocional registrando cada decisión clave",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Selector de día
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = CardDefaults.outlinedShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.goToPreviousDay() }) {
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
                                onClick = { viewModel.goToNextDay() },
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

                // Botón para habilitar / cancelar modo borrar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.toggleDeleteMode() }) {
                            Text(
                                text = if (uiState.isDeleteMode)
                                    "Cancelar selección"
                                else
                                    "Seleccionar para borrar"
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (uiState.decisions.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyDayCard()
                    }
                } else {
                    items(uiState.decisions, key = { it.id }) { decision ->
                        val isSelected = uiState.selectedForDeletion.contains(decision.id)
                        DecisionCard(
                            decision = decision,
                            isDeleteMode = uiState.isDeleteMode,
                            isSelected = isSelected,
                            onCardClick = { onDecisionClick(decision.id) },
                            onToggleSelection = { viewModel.toggleDecisionSelection(decision.id) }
                        )
                    }
                }
            }

            // FAB "+" flotante para nueva decisión
            FloatingActionButton(
                onClick = onAddDecisionClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Nueva decisión",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Botón de eliminar cuando hay elementos seleccionados
            if (uiState.isDeleteMode && uiState.selectedForDeletion.isNotEmpty()) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Eliminar (${uiState.selectedForDeletion.size})")
                }
            }

            // Diálogo de confirmación de borrado
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionCard(
    decision: Decision,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    // Color según intensidad 1..5 (verde claro -> verde oscuro)
    val intensityColor = when (decision.intensity) {
        1 -> Color(0xFFC8E6C9) // muy claro
        2 -> Color(0xFFA5D6A7)
        3 -> Color(0xFF81C784)
        4 -> Color(0xFF4CAF50)
        5 -> Color(0xFF2E7D32) // oscuro
        else -> Color(0xFFC8E6C9)
    }

    val clickAction = if (isDeleteMode) onToggleSelection else onCardClick

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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fila superior: checkbox cuando está en modo borrar
            if (isDeleteMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Seleccionar",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Texto de la decisión
            Text(
                text = decision.text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Categoría
            Text(
                text = decision.category.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Emociones + intensidad con color
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                decision.emotions.take(2).forEach { emotion ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(emotion.displayName) }
                    )
                }

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("Intensidad ${decision.intensity}/5") },
                    leadingIcon = {
                        val icon =
                            if (decision.intensity >= 3) Icons.Filled.ArrowCircleUp else Icons.Filled.ArrowCircleDown
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
