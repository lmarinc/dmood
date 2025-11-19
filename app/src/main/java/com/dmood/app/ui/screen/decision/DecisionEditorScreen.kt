package com.dmood.app.ui.screen.decision

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.ui.DmoodViewModelFactory
import kotlin.math.roundToInt

private val categoryColors = mapOf(
    CategoryType.TRABAJO_ESTUDIOS to Color(0xFFDDEBFF),
    CategoryType.SALUD_BIENESTAR to Color(0xFFC8F7C5),
    CategoryType.RELACIONES_SOCIAL to Color(0xFFFFE1E0),
    CategoryType.FINANZAS_COMPRAS to Color(0xFFFFF2CC),
    CategoryType.HABITOS_CRECIMIENTO to Color(0xFFE6D8FF),
    CategoryType.OCIO_TIEMPO_LIBRE to Color(0xFFD4F0FF),
    CategoryType.CASA_ORGANIZACION to Color(0xFFFFE5D0),
    CategoryType.OTRO to Color(0xFFE0E0E0)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DecisionEditorScreen(
    decisionId: Long = 0L,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DecisionEditorViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(decisionId) {
        if (decisionId > 0) {
            viewModel.loadDecision(decisionId)
        }
    }

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
                title = { Text(if (uiState.isEditing) "Editar decisión" else "Nueva decisión") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StepIndicator(currentStep = uiState.currentStep)
                when (uiState.currentStep) {
                    1 -> StepOne(
                        text = uiState.text,
                        onTextChange = viewModel::onTextChange,
                        onNext = viewModel::goToNextStep
                    )

                    2 -> StepTwo(
                        selected = uiState.selectedEmotions,
                        onToggleEmotion = viewModel::onToggleEmotion,
                        intensity = uiState.intensity,
                        onIntensityChange = viewModel::onIntensityChange,
                        onNext = viewModel::goToNextStep,
                        onBack = viewModel::goToPreviousStep
                    )

                    3 -> StepThree(
                        category = uiState.category,
                        onCategoryChange = viewModel::onCategoryChange,
                        summaryText = uiState.text,
                        emotions = uiState.selectedEmotions,
                        intensity = uiState.intensity,
                        onBack = viewModel::goToPreviousStep,
                        onSave = viewModel::saveDecision,
                        isSaving = uiState.isSaving,
                        onDelete = { showDeleteDialog = true },
                        canDelete = uiState.isEditing
                    )
                }

                if (uiState.validationError != null) {
                    Text(
                        text = uiState.validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Borrar esta decisión?") },
            text = { Text("La decisión desaparecerá del registro de forma permanente.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteCurrentDecision()
                }) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        (1..3).forEach { step ->
            val isActive = step <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun StepOne(
    text: String,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Paso 1 · Contexto", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Describe la decisión o momento que quieres registrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Ej. Acepté una nueva propuesta de trabajo") },
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onNext,
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepTwo(
    selected: Set<EmotionType>,
    onToggleEmotion: (EmotionType) -> Unit,
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Paso 2 · Emociones", style = MaterialTheme.typography.titleMedium)
                EmotionWheel(selected = selected, onToggleEmotion = onToggleEmotion)
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Intensidad", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = intensity.toFloat(),
                    onValueChange = { onIntensityChange(it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("1 · Suave")
                    Text("3 · Media")
                    Text("5 · Intensa")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("Siguiente")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepThree(
    category: CategoryType?,
    onCategoryChange: (CategoryType) -> Unit,
    summaryText: String,
    emotions: Set<EmotionType>,
    intensity: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Paso 3 · Categoría", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryType.values().forEach { type ->
                        val selected = type == category
                        CategoryChip(
                            text = type.displayName,
                            color = categoryColors[type] ?: MaterialTheme.colorScheme.surfaceVariant,
                            selected = selected,
                            onClick = { onCategoryChange(type) }
                        )
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Resumen rápido", style = MaterialTheme.typography.titleMedium)
                Text(text = summaryText.ifBlank { "Aún sin descripción" }, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Emociones: ${if (emotions.isEmpty()) "Sin seleccionar" else emotions.joinToString { it.displayName }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = "Intensidad: $intensidad/5", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Categoría: ${category?.displayName ?: "Sin definir"}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar")
            }
        }
        if (canDelete) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Borrar decisión")
            }
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) color.copy(alpha = 0.9f) else color.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmotionWheel(
    selected: Set<EmotionType>,
    onToggleEmotion: (EmotionType) -> Unit
) {
    val layout = listOf(
        listOf(EmotionType.ALEGRE, EmotionType.SORPRENDIDO, EmotionType.MOTIVADO),
        listOf(EmotionType.SEGURO, EmotionType.NORMAL, EmotionType.MIEDO),
        listOf(EmotionType.TRISTE, EmotionType.INCOMODO, EmotionType.ENFADADO)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        layout.forEachIndexed { rowIndex, rowEmotions ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowEmotions.forEach { emotion ->
                    val isCenter = emotion == EmotionType.NORMAL && rowIndex == 1
                    val isSelected = selected.contains(emotion)
                    val size = if (isCenter) 80.dp else 56.dp
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) emotion.color else emotion.color.copy(alpha = 0.4f)
                            )
                            .clickable { onToggleEmotion(emotion) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emotion.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
