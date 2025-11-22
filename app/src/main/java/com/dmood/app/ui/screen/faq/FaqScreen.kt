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
        title = "Qué tipo de decisiones registrar",
        body = "Anota momentos con carga emocional o relevancia personal: conversaciones difíciles, avances, dudas o micro logros."
    ),
    FaqEntry(
        title = "Qué no tiene sentido registrar",
        body = "Acciones totalmente automáticas o sin impacto emocional (ej. cepillarte los dientes) no aportan contexto útil."
    ),
    FaqEntry(
        title = "Emociones y cómo usarlas",
        body = "Puedes elegir hasta dos emociones (NORMAL es exclusiva). Sus colores te ayudarán a reconocer patrones de energía."
    ),
    FaqEntry(
        title = "Impulsivo vs calmado",
        body = "El tono se calcula automáticamente: emociones intensas y negativas tienden a marcar decisiones impulsivas, las positivas a calmadas."
    ),
    FaqEntry(
        title = "Cómo interpretar tu resumen semanal",
        body = "Verás porcentajes calmadas/impulsivas, días más luminosos o retadores y las áreas donde concentras energía."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DmoodTopBar(
                title = "Guía D-Mood"
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
