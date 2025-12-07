package com.dmood.app.ui.screen.faq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmood.app.ui.components.DmoodTopBar

private data class FaqEntry(val title: String, val body: String)

private val faqEntries = listOf(
    FaqEntry(
        title = "Qué registrar en \"Hoy\"",
        body = "Cada decisión guarda un texto breve, hasta dos emociones (Normal es exclusiva), la intensidad que sentiste y la categoría de tu vida donde encaja. Así el resumen semanal se nutre de contexto real."
    ),
    FaqEntry(
        title = "Cómo usar las emociones",
        body = "Elige hasta dos emociones y marca su intensidad. Te servirán para recordar el momento y para detectar patrones de energía a lo largo de la semana."
    ),
    FaqEntry(
        title = "Categorías y filtros",
        body = "Elige una categoría (Trabajo/Estudios, Salud/Bienestar, Relaciones, Finanzas, Hábitos, Ocio, Casa u Otro) para dar contexto. Desde la pantalla de Hoy puedes filtrar por categoría y cambiar entre tarjetas compactas o amplias."
    ),
    FaqEntry(
        title = "Editar o borrar decisiones",
        body = "Toca una tarjeta para editarla. El modo borrar solo aparece en el día actual: activa la papelera, selecciona varias decisiones y confirma. Días pasados quedan bloqueados para evitar perder historial."
    ),
    FaqEntry(
        title = "Cuándo se genera el resumen semanal",
        body = "El corte se basa en tu día de inicio de semana (Ajustes). Tras los primeros 3 días de uso, cada semana se abre un nuevo resumen con categorías, emociones destacadas y evolución por días."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Guía D-Mood",
                subtitle = "Consejos rápidos",
                showLogo = true
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(faqEntries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = entry.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
