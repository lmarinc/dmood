package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.usecase.WeeklyHighlight
import com.dmood.app.domain.usecase.WeeklySummary
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Text("Cargando resumen semanal…")
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.summary != null && uiState.highlight != null -> {
                    SummaryDistributionCard(uiState.summary)
                    HighlightsCard(uiState.highlight)
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

@Composable
private fun SummaryDistributionCard(summary: WeeklySummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Distribución de decisiones",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Calmadas: ${summary.calmPercentage.toInt()}%")
            Text("Impulsivas: ${summary.impulsivePercentage.toInt()}%")
            Text("Neutras: ${summary.neutralPercentage.toInt()}%")
        }
    }
}

@Composable
private fun HighlightsCard(highlight: WeeklyHighlight) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lo más destacable",
                style = MaterialTheme.typography.titleMedium
            )

            highlight.strongestPositiveDay?.let { day ->
                Text("Día con mejor tono: $day")
            }

            highlight.strongestNegativeDay?.let { day ->
                Text("Día con más reto: $day")
            }

            highlight.mostFrequentCategory?.let { category ->
                Text("Área más presente: ${category.displayName}")
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(highlight.emotionalTrend)
        }
    }
}
