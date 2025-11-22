package com.dmood.app.ui.screen.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklyHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: WeeklyHistoryViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Histórico semanal"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.feedbackMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (uiState.entries.isEmpty() && !uiState.isLoading) {
                Text(
                    text = "Aún no hay resúmenes para descargar.",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.entries) { entry ->
                    val start = Instant.ofEpochMilli(entry.windowStart).atZone(zone).toLocalDate()
                    val end = Instant.ofEpochMilli(entry.windowEnd).atZone(zone).toLocalDate()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Semana del ${start.format(formatter)} al ${end.format(formatter)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            entry.summary?.let { summary ->
                                Text(
                                    text = "Calmadas: ${summary.calmPercentage.toInt()}%  ·  Impulsivas: ${summary.impulsivePercentage.toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val topCategory = summary.categoryDistribution.entries.maxByOrNull { it.value }
                                topCategory?.let {
                                    Text(
                                        text = "Área predominante: ${it.key.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { viewModel.generatePdf(context, entry) }) {
                                    Text("Descargar PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
