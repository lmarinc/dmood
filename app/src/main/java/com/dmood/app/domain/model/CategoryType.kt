package com.dmood.app.domain.model

/**
 * Categorías principales para registrar y analizar eventos o hábitos en D-Mood.
 */
enum class CategoryType(
    val displayName: String,
    val description: String,
) {
    /** Aspectos relacionados al desempeño profesional o académico. */
    TRABAJO_ESTUDIOS(
        displayName = "Trabajo/Estudios",
        description = "Desafíos y logros laborales o académicos",
    ),

    /** Cuidado físico y mental, así como hábitos saludables. */
    SALUD_BIENESTAR(
        displayName = "Salud/Bienestar",
        description = "Rutinas y acciones para cuidar cuerpo y mente",
    ),

    /** Vínculos familiares, amistades y comunidad. */
    RELACIONES_SOCIAL(
        displayName = "Relaciones/Social",
        description = "Calidad de interacciones y vínculos personales",
    ),

    /** Manejo de dinero, gastos y objetivos financieros. */
    FINANZAS_COMPRAS(
        displayName = "Finanzas/Compras",
        description = "Organización de ingresos, gastos y compras",
    ),

    /** Desarrollo personal, hábitos y aprendizaje constante. */
    HABITOS_CRECIMIENTO(
        displayName = "Hábitos/Crecimiento",
        description = "Metas, rutinas y progresos de crecimiento personal",
    ),

    /** Tiempo libre, entretenimiento y actividades recreativas. */
    OCIO_TIEMPO_LIBRE(
        displayName = "Ocio/Tiempo libre",
        description = "Actividades de ocio para recargar energía",
    ),

    /** Organización del hogar y responsabilidades domésticas. */
    CASA_ORGANIZACION(
        displayName = "Casa/Organización",
        description = "Tareas domésticas y orden del entorno personal",
    ),

    /** Eventos o situaciones que no encajan en otras categorías. */

    OTRO(
        displayName = "Otro",
        description = "Situaciones especiales o fuera de las demás categorías",
    );

    /**
     * Indica si la categoría se relaciona al crecimiento o bienestar personal.
     */
    fun isPersonalGrowthRelated(): Boolean = when (this) {
        SALUD_BIENESTAR,
        HABITOS_CRECIMIENTO,
        RELACIONES_SOCIAL,
        OCIO_TIEMPO_LIBRE -> true
        else -> false
    }
}