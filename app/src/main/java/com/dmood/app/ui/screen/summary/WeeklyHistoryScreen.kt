package com.dmood.app.ui.screen.summary

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklyHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyHistoryViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var feedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        feedbackMessage = null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Histórico semanal",
                showLogo = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                uiState.error != null -> {
                    item {
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.weeks.isEmpty() -> {
                    item {
                        Text(
                            text = "Todavía no hay semanas registradas para exportar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.weeks) { week ->
                WeeklyHistoryCard(
                    week = week,
                    onExport = {
                        val file = viewModel.exportWeekToPdf(context, week)
                        feedbackMessage = if (file != null) {
                            "PDF guardado en ${file.name}"
                        } else {
                            "No hay datos suficientes para exportar esta semana"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WeeklyHistoryCard(
    week: WeeklyHistoryItem,
    onExport: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
    val startText = week.startDate.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    val endText = week.endDate.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

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
                text = week.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Del $startText al $endText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (week.hasData) "Resumen listo para descargar" else "Sin suficientes decisiones esta semana.",
                style = MaterialTheme.typography.bodySmall,
                color = if (week.hasData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onExport,
                enabled = week.hasData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (week.hasData) "Descargar PDF" else "Pendiente de datos")
            }
        }
    }
}
