package com.dmood.app.ui.screen.decision

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.EmotionType
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.theme.DmoodTheme
import com.dmood.app.ui.theme.toUiColor
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun DecisionEditorScreen(
    decisionId: Long = 0L,
    onClose: () -> Unit,
    onSaved: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DecisionEditorViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(decisionId) {
        if (decisionId > 0) {
            viewModel.loadDecision(decisionId)
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            viewModel.resetSavedFlag()
            onSaved()
            onClose()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        Text(
                            text = if (uiState.isEditing) "Editar decisión" else "Nueva decisión",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Paso ${uiState.currentStep} de 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cerrar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
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
                StepProgressRow(totalSteps = 3, currentStep = uiState.currentStep)

                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val baseTween = tween<IntOffset>(durationMillis = 420, easing = FastOutSlowInEasing)

                        if (forward) {
                            (slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = baseTween
                            ) + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = baseTween
                                ) + fadeOut(tween(240)))
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { -it / 2 },
                                animationSpec = baseTween
                            ) + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { it / 3 },
                                    animationSpec = baseTween
                                ) + fadeOut(tween(240)))
                        }
                    },
                    label = "step-animation"
                ) { step ->
                    when (step) {
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
                            isSaving = uiState.isSaving
                        )
                    }
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
}

@Composable
private fun StepProgressRow(totalSteps: Int, currentStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { index ->
                val color by animateColorAsState(
                    targetValue = if (index + 1 <= currentStep) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    label = "step-indicator-color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color)
                )
            }
        }
        Text(
            text = "Paso ${currentStep} de $totalSteps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepOne(
    text: String,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Contexto",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Describe la decisión o momento que quieres registrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Ej. Acepté una nueva propuesta de trabajo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp, max = 180.dp),
                maxLines = 5,
                shape = MaterialTheme.shapes.large
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

@Composable
private fun StepTwo(
    selected: Set<EmotionType>,
    onToggleEmotion: (EmotionType) -> Unit,
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Emociones",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Elige hasta dos emociones. Toca sobre el círculo para activarlas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EmotionGrid(
                    selected = selected,
                    onToggleEmotion = onToggleEmotion
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
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
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1 · Suave")
                    Text("3 · Media")
                    Text("5 · Intensa")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("Siguiente")
            }
        }
    }
}

@Composable
private fun StepThree(
    category: CategoryType?,
    onCategoryChange: (CategoryType) -> Unit,
    summaryText: String,
    emotions: Set<EmotionType>,
    intensity: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleMedium
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Resumen rápido",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = summaryText.ifBlank { "Aún sin descripción" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoPill(
                        label = "Emociones",
                        value = if (emotions.isEmpty())
                            "Sin seleccionar"
                        else
                            emotions.joinToString { it.displayName }
                    )
                    InfoPill(
                        label = "Intensidad",
                        value = "$intensity / 5"
                    )
                    InfoPill(
                        label = "Categoría",
                        value = category?.displayName ?: "Sin definir"
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
    }
}

@Composable
private fun InfoPill(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
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

/**
 * Emociones organizadas de forma más estética:
 *
 * Fila 1: Alegre – Motivado – Sorprendido
 * Fila 2: Seguro – Normal – Miedo
 * Fila 3: Triste – Incómodo – Enfadado
 */
@Composable
private fun EmotionGrid(
    selected: Set<EmotionType>,
    onToggleEmotion: (EmotionType) -> Unit
) {
    val rows: List<List<EmotionType>> = listOf(
        listOf(EmotionType.ALEGRE, EmotionType.MOTIVADO, EmotionType.SORPRENDIDO),
        listOf(EmotionType.SEGURO, EmotionType.NORMAL, EmotionType.MIEDO),
        listOf(EmotionType.TRISTE, EmotionType.INCOMODO, EmotionType.ENFADADO)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEachIndexed { rowIndex, rowEmotions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowEmotions.forEach { emotion ->
                    val isCentral = (rowIndex == 1 && emotion == EmotionType.NORMAL)
                    EmotionItem(
                        emotion = emotion,
                        isCentral = isCentral,
                        isSelected = selected.contains(emotion),
                        onToggleEmotion = onToggleEmotion
                    )
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
    val targetColor = if (isSelected) emotion.color else emotion.color.copy(alpha = 0.40f)
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        label = "emotion-color"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1f,
        label = "emotion-scale"
    )
    val interactionSource = remember { MutableInteractionSource() }
    
    val textColor = if (emotion == EmotionType.MIEDO && isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(if (isCentral) 95.dp else 75.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isSelected) 14.dp else 6.dp,
                shape = CircleShape,
                ambientColor = emotion.color.copy(alpha = 0.35f),
                spotColor = emotion.color.copy(alpha = 0.45f)
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = { onToggleEmotion(emotion) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emotion.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepTwoPreview() {
    DmoodTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StepTwo(
                selected = setOf(EmotionType.ALEGRE, EmotionType.MIEDO),
                onToggleEmotion = {},
                intensity = 3,
                onIntensityChange = {},
                onNext = {},
                onBack = {}
            )
        }
    }
}
