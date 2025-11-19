package com.dmood.app.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.dmood.app.ui.DmoodViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddDecisionClick: () -> Unit,
    onOpenSummaryClick: () -> Unit,
    onDecisionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // Este bloque se ejecuta cada vez que Home entra en composición
    LaunchedEffect(Unit) {
        viewModel.loadTodayDecisions()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = "Hoy") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Decisiones de hoy",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(
                onClick = onAddDecisionClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nueva decisión")
            }
            Button(
                onClick = onOpenSummaryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver resumen semanal")
            }

            when {
                uiState.isLoading -> {
                    Text("Cargando decisiones…")
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.todayDecisions.isEmpty() -> {
                    Text(
                        text = "Hoy aún no has registrado decisiones. Empieza con una nueva decisión.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.todayDecisions, key = { it.id }) { decision ->
                            DecisionCard(
                                decision = decision,
                                onClick = onDecisionClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionCard(
    decision: com.dmood.app.domain.model.Decision,
    onClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(decision.id) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = decision.text,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Área: ${decision.category.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Tono: ${decision.tone.toDisplayName()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun com.dmood.app.domain.model.DecisionTone.toDisplayName(): String = when (this) {
    com.dmood.app.domain.model.DecisionTone.CALMADA -> "Calmada"
    com.dmood.app.domain.model.DecisionTone.IMPULSIVA -> "Impulsiva"
    com.dmood.app.domain.model.DecisionTone.NEUTRA -> "Neutra"
}
