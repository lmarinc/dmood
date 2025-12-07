package com.dmood.app.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.ui.DmoodViewModelFactory
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private data class IntroStep(val title: String, val description: String)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) {
            onFinishOnboarding()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        val selectedWeekStart = uiState.selectedWeekStartDay ?: uiState.weekStartDay
        val locale = remember { Locale("es", "ES") }
        val weekStartName = selectedWeekStart.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        val introSteps = listOf(
            IntroStep(
                title = "Registra tu día a día",
                description = "En 'Hoy' añades decisiones rápidas con emociones e intensidad para recordar mejor el contexto."
            ),
            IntroStep(
                title = "Organiza con categorías y filtros",
                description = "Agrupa por áreas como Trabajo, Salud o Relaciones y filtra en la pantalla principal para enfocarte en lo que importa."
            ),
            IntroStep(
                title = "Explora tu resumen semanal",
                description = "Verás tendencias, emociones que se repiten y las categorías donde concentras energía."
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 56.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Bienvenido a Dmood",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Un micro diario para reconocer tus decisiones y el tono emocional que las acompaña.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StepIndicator(totalSteps = introSteps.size + 1, currentStep = currentStep)

                when (currentStep) {
                    0 -> PersonalInfoStep(
                        name = uiState.name,
                        onNameChange = viewModel::onNameChange,
                        selectedWeekStart = selectedWeekStart,
                        onWeekStartSelected = viewModel::onWeekStartSelected,
                        weekStartName = weekStartName,
                        errorMessage = uiState.errorMessage
                    )

                    else -> GuideStepCard(step = introSteps[currentStep - 1])
                }

                if (currentStep > 0) {
                    Text(
                        text = "Tu semana empieza en $weekStartName. Puedes ajustarlo más adelante en Ajustes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(onClick = { currentStep -= 1 }) {
                            Text("Anterior")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(0.dp))
                    }

                    val isLastStep = currentStep == introSteps.lastIndex + 1
                    Button(
                        onClick = {
                            if (isLastStep) {
                                viewModel.completeOnboarding()
                            } else {
                                currentStep += 1
                            }
                        },
                        enabled = if (currentStep == 0) {
                            uiState.name.isNotBlank() && uiState.selectedWeekStartDay != null && !uiState.isSaving
                        } else !uiState.isSaving
                    ) {
                        if (uiState.isSaving && isLastStep) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.padding(4.dp)
                            )
                        } else {
                            Text(if (isLastStep) "Empezar" else "Siguiente")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(totalSteps: Int, currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isActive) 10.dp else 6.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

@Composable
private fun PersonalInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    selectedWeekStart: DayOfWeek,
    onWeekStartSelected: (DayOfWeek) -> Unit,
    weekStartName: String,
    errorMessage: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Configura tu espacio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("¿Cómo te llamas?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "¿En qué día arranca tu semana?",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.values().forEach { day ->
                    val selected = selectedWeekStart == day
                    AssistChip(
                        onClick = { onWeekStartSelected(day) },
                        label = {
                            Text(
                                day.getDisplayName(TextStyle.SHORT, Locale("es", "ES")),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Text(
                text = "Usaremos $weekStartName para abrir tus resúmenes. Cambiarlo recalculará las semanas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun GuideStepCard(step: IntroStep) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
