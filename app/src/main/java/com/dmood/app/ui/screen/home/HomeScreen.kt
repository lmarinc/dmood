package com.dmood.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.screen.home.HomeViewModel
import kotlinx.coroutines.flow.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddDecisionClick: () -> Unit,
    onOpenSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
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
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hoy",
                style = MaterialTheme.typography.headlineMedium
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
            uiState.todayDecisions.forEach { decision ->
                Text(
                    text = "• ${decision.text}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
