package com.dmood.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.dmood.app.domain.model.CategoryType

fun CategoryType.toUiColor(): Color = when (this) {
    CategoryType.TRABAJO_ESTUDIOS -> Color(0xFFDDEBFF)
    CategoryType.SALUD_BIENESTAR -> Color(0xFFC8F7C5)
    CategoryType.RELACIONES_SOCIAL -> Color(0xFFFFE1E0)
    CategoryType.FINANZAS_COMPRAS -> Color(0xFFFFF2CC)
    CategoryType.HABITOS_CRECIMIENTO -> Color(0xFFE6D8FF)
    CategoryType.OCIO_TIEMPO_LIBRE -> Color(0xFFD4F0FF)
    CategoryType.CASA_ORGANIZACION -> Color(0xFFFFE5D0)
    CategoryType.OTRO -> Color(0xFFE0E0E0)
}
