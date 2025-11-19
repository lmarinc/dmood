// kotlin
package com.dmood.app.ui.screen.decision

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.ripple.rememberRipple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.theme.toUiColor
import kotlin.math.roundToInt
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DecisionEditorScreen(
    decisionId: Long = 0L,
    onClose: () -> Unit,
    onSaved: () -> Unit = {}, // callback añadido
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
            onSaved()   // notifico que se guardó
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
                    onSaved()   // notifico que hubo un cambio (eliminar)
                    onClose()
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
    val steps = listOf(
        "Contexto",
        "Emociones",
        "Categoría"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isActive = currentStep >= stepNumber
            val isCurrent = currentStep == stepNumber
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (index != steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .height(2.dp)
                                .weight(1f)
                                .background(
                                    if (currentStep > stepNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
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
                Text(
                    text = "Elige hasta dos emociones. Toca sobre el círculo para activarlas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            color = type.toUiColor(),
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
                Text(text = "Intensidad: $intensity/5", style = MaterialTheme.typography.bodyMedium)
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
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.95f) else color.copy(alpha = 0.6f),
        label = "category-chip-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "category-chip-border"
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .border(width = if (selected) 1.5.dp else 1.dp, color = borderColor, shape = MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val diameter = min(maxWidth, 320.dp)
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                layout.forEachIndexed { index, rowEmotions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowEmotions.forEach { emotion ->
                            EmotionItem(
                                emotion = emotion,
                                isCentral = emotion == EmotionType.NORMAL && index == 1,
                                isSelected = selected.contains(emotion),
                                onToggleEmotion = onToggleEmotion
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionItem(
    emotion: EmotionType,
    isCentral: Boolean,
    isSelected: Boolean,
    onToggleEmotion: (EmotionType) -> Unit
) {
    val targetColor = if (isSelected) emotion.color else emotion.color.copy(alpha = 0.45f)
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "emotion-color")
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1f, label = "emotion-scale")
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(if (isCentral) 90.dp else 64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, radius = 50.dp)
            ) { onToggleEmotion(emotion) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emotion.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}
