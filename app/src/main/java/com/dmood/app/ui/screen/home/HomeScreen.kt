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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.ui.DmoodViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentDay()
    }

    val locale = remember { Locale("es", "ES") }
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, dd/MM", locale) }
    val formattedDate = remember(uiState.selectedDate) {
        uiState.selectedDate.format(formatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }
    val isToday = uiState.selectedDate == LocalDate.now()

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
                                Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Día anterior")
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
                                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Día siguiente")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onAddDecisionClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nueva decisión")
                        }
                        OutlinedButton(
                            onClick = onOpenSummaryClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resumen semanal")
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

                if (uiState.errorMessage != null) {
                    item {
                        AssistChip(
                            onClick = viewModel::refreshCurrentDay,
                            label = { Text(uiState.errorMessage ?: "") }
                        )
                    }
                }

                if (uiState.decisions.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyDayCard()
                    }
                } else {
                    items(uiState.decisions, key = { it.id }) { decision ->
                        DecisionCard(
                            decision = decision,
                            onClick = { onDecisionClick(decision.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
    onClick: () -> Unit
) {
    val toneColor = when (decision.tone) {
        DecisionTone.CALMADA -> Color(0xFF7CC5A1)
        DecisionTone.IMPULSIVA -> Color(0xFFFF8B6A)
        DecisionTone.NEUTRA -> Color(0xFFB0BEC5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .weight(1f)
                        .background(
                            color = toneColor,
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (decision.tone) {
                        DecisionTone.CALMADA -> "Calmada"
                        DecisionTone.IMPULSIVA -> "Impulsiva"
                        DecisionTone.NEUTRA -> "Neutra"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = toneColor
                )
            }

            Text(
                text = decision.text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = decision.category.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                decision.emotions.take(2).forEach { emotion ->
                    AssistChip(
                        onClick = {},
                        label = { Text(emotion.displayName) },
                        enabled = false
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text("Intensidad ${decision.intensity}/5") },
                    leadingIcon = {
                        val icon = if (decision.intensity >= 3) Icons.Filled.ArrowCircleUp else Icons.Filled.ArrowCircleDown
                        Icon(icon, contentDescription = null)
                    },
                    enabled = false
                )
            }
        }
    }
}
