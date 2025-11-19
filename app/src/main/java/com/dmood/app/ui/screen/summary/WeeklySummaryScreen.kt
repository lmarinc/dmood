package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.ui.DmoodViewModelFactory

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Resumen semanal") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            when {
                uiState.isLoading -> {
                    Text(
                        text = "Cargando resumen semanal…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.summary != null && uiState.highlight != null -> {
                    val summary = uiState.summary!!
                    val highlight = uiState.highlight!!

                    // Bloque 1: Distribución de decisiones (solo calmadas / impulsivas)
                    Text(
                        text = "Distribución de decisiones",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Calmadas: ${summary.calmPercentage.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Impulsivas: ${summary.impulsivePercentage.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            // No mostramos las neutras, aunque existan en el modelo
                        }
                    }

                    // Bloque 2: Lo más destacable
                    Text(
                        text = "Lo más destacable",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            highlight.strongestPositiveDay?.let { day ->
                                Text(
                                    text = "Día con mejor tono: $day",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            highlight.strongestNegativeDay?.let { day ->
                                Text(
                                    text = "Día con más reto: $day",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            highlight.mostFrequentCategory?.let { category ->
                                Text(
                                    text = "Área más presente: ${category.displayName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = highlight.emotionalTrend,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Todavía no hay suficientes decisiones esta semana para generar un resumen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
