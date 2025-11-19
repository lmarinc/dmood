package com.dmood.app.ui.screen.decision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.ui.DmoodViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionEditorScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DecisionEditorViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Editor de decisión") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
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
            Text(
                text = "Describe brevemente tu decisión",
                style = MaterialTheme.typography.titleMedium
            )

            // Campo de texto para la decisión
            TextField(
                value = uiState.text,
                onValueChange = { viewModel.onTextChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Acepté una nueva propuesta de trabajo") },
                singleLine = false,
                maxLines = 4
            )

            Text(
                text = "¿Cómo te sentías en esta decisión?",
                style = MaterialTheme.typography.titleMedium
            )

            // Selección de emociones
            EmotionChips(
                selected = uiState.selectedEmotions,
                onToggleEmotion = { viewModel.onToggleEmotion(it) }
            )

            if (uiState.validationError != null) {
                Text(
                    text = uiState.validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { viewModel.saveDecision() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text("Guardar")
            }

            if (uiState.savedSuccessfully) {
                LaunchedEffect(uiState.savedSuccessfully) {
                    viewModel.resetSavedFlag()
                    onClose()
                }
            }
        }
    }
}

@Composable
private fun EmotionChips(
    selected: Set<EmotionType>,
    onToggleEmotion: (EmotionType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val emotions = EmotionType.values().toList()
        val chunkSize = 3

        emotions.chunked(chunkSize).forEach { rowEmotions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowEmotions.forEach { emotion ->
                    val isSelected = selected.contains(emotion)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleEmotion(emotion) },
                        label = { Text(emotion.displayName) }
                    )
                }
            }
        }
    }
}
