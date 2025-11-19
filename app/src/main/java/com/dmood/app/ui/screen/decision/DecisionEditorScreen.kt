// Kotlin
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.ui.DmoodViewModelFactory
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionEditorScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DecisionEditorViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 1) Texto de la decisión
            Text(
                text = "Describe brevemente tu decisión",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = uiState.text,
                onValueChange = { viewModel.onTextChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Acepté una nueva propuesta de trabajo") },
                maxLines = 4,
                singleLine = false,

                )

            // 2) Emociones
            Text(
                text = "¿Cómo te sentías en esta decisión?",
                style = MaterialTheme.typography.titleMedium
            )

            EmotionChips(
                selected = uiState.selectedEmotions,
                onToggleEmotion = { viewModel.onToggleEmotion(it) }
            )

            // 3) Intensidad
            Text(
                text = "Intensidad (1 a 5)",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "¿Cuán intensa fue esta decisión para ti?",
                style = MaterialTheme.typography.bodySmall
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    value = uiState.intensity.toFloat(),
                    onValueChange = { value ->
                        viewModel.onIntensityChange(value.roundToInt())
                    },
                    valueRange = 1f..5f,
                    steps = 3 // puntos intermedios: 2,3,4
                )
                Text(
                    text = "Intensidad actual: ${uiState.intensity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 4) Categoría
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium
            )

            CategoryDropdown(
                selectedCategory = uiState.category,
                onCategorySelected = { viewModel.onCategoryChange(it) }
            )

            // 5) Error de validación (si lo hay)
            if (uiState.validationError != null) {
                Text(
                    text = uiState.validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 6) Botón guardar
            Button(
                onClick = { viewModel.saveDecision() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text("Guardar")
            }

            // 7) Cierre automático al guardar correctamente
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: CategoryType?,
    onCategorySelected: (CategoryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val allCategories = CategoryType.values().toList()
    val label = selectedCategory?.displayName ?: "Elige una categoría"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Categoría") },
            trailingIcon = {
                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = androidx.compose.material3.ExposedDropdownMenuDefaults.textFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allCategories.forEach { category ->
                androidx.compose.material3.DropdownMenuItem(
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
