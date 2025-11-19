package com.dmood.app.ui.screen.decision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.ui.DmoodViewModelFactory
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionEditorScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DecisionEditorViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            viewModel.resetSavedFlag()
            onClose()
        }
    }

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
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = "Describe brevemente tu decisión",
                style = MaterialTheme.typography.titleMedium
            )

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

            EmotionChips(
                selected = uiState.selectedEmotions,
                onToggleEmotion = { viewModel.onToggleEmotion(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Intensidad",
                style = MaterialTheme.typography.titleMedium
            )
            Text("¿Cuán intensa fue esta decisión para ti?")
            Slider(
                value = uiState.intensity.toFloat(),
                onValueChange = { viewModel.onIntensityChange(it.roundToInt()) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Nivel seleccionado: ${uiState.intensity}/5")

            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium
            )

            CategoryDropdown(
                selected = uiState.category,
                onCategorySelected = { viewModel.onCategoryChange(it) }
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
                Text(if (uiState.decisionId != null) "Actualizar decisión" else "Guardar")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: CategoryType?,
    onCategorySelected: (CategoryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = CategoryType.values().toList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected?.displayName ?: "Selecciona una categoría",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            placeholder = { Text("Categoría principal") }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
