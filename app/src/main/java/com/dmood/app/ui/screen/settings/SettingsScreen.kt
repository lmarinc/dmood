package com.dmood.app.ui.screen.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.components.DmoodTopBar
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenWeeklyHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = DmoodViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var pendingWeekStart by remember { mutableStateOf<DayOfWeek?>(null) }
    var pendingReminderAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                }

                is SettingsEvent.ConfirmWeekStartChange -> {
                    pendingWeekStart = event.dayOfWeek
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                pendingReminderAction?.invoke()
            }
            pendingReminderAction = null
        }
    )

    fun ensureNotificationPermission(onGranted: () -> Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            pendingReminderAction = onGranted
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onGranted()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Ajustes",
                subtitle = "Personaliza tu espacio",
                showLogo = true
                // sin botón de cerrar ni acciones extra
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Eliminado el título "Tu espacio personal" para ganar espacio

            SettingsSection(title = "Cuenta") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.editedName,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Nombre mostrado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = viewModel::saveName,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar nombre")
                    }
                    if (uiState.feedbackMessage != null) {
                        AssistChip(
                            onClick = viewModel::clearFeedback,
                            label = { Text(uiState.feedbackMessage ?: "") }
                        )
                    }
                }
            }

            SettingsSection(title = "Recordatorios") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReminderRow(
                        label = "Recordatorio diario",
                        checked = uiState.dailyReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                ensureNotificationPermission {
                                    viewModel.setDailyReminderEnabled(true)
                                }
                            } else {
                                viewModel.setDailyReminderEnabled(false)
                            }
                        }
                    )
                    ReminderRow(
                        label = "Resumen semanal",
                        checked = uiState.weeklyReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                ensureNotificationPermission {
                                    viewModel.setWeeklyReminderEnabled(true)
                                }
                            } else {
                                viewModel.setWeeklyReminderEnabled(false)
                            }
                        }
                    )
                    Text(
                        text = "Te avisaremos a las 22:00 si falta tu registro y cuando haya un nuevo resumen semanal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsSection(title = "Inicio de semana") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Elige el día en el que comienza tu semana de seguimiento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.values().forEach { day ->
                            val selected = uiState.weekStartDay == day
                            AssistChip(
                                onClick = { viewModel.onWeekStartClick(day) },
                                label = {
                                    Text(
                                        day.getDisplayName(TextStyle.SHORT, Locale("es", "ES")),
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null
                                        )
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    labelColor = if (selected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    Text(
                        text = "Esto ajusta cuándo se desbloquean los resúmenes semanales.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

//            SettingsSection(title = "Histórico semanal") {
//                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                    Text(
//                        text = "Descarga los resúmenes semanales en PDF para revisarlos cuando quieras.",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Button(
//                        onClick = onOpenWeeklyHistory,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Abrir histórico")
//                    }
//                }
//            }

            // Espacio extra al final para que las últimas tarjetas no queden “pegadas” al borde inferior
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    pendingWeekStart?.let { day ->
        val formattedDay = remember(day) {
            val raw = day.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
            raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
        }

        AlertDialog(
            onDismissRequest = { pendingWeekStart = null },
            title = { Text("Cambiar inicio de semana") },
            text = {
                Text(
                    "El resumen semanal se recalculará usando $formattedDay. Solo puedes modificar este día una vez al mes. ¿Quieres continuar?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingWeekStart = null
                        viewModel.confirmWeekStartChange(day)
                    }
                ) {
                    Text("Confirmar cambio")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingWeekStart = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // blanco, como en Home
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ReminderRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (checked) "Activado" else "Pendiente",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
