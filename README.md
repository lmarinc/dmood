# D-Mood

D-Mood es un micro diario de decisiones y emociones construido con Kotlin y Jetpack Compose. La app permite registrar decisiones del día, etiquetar emociones y categorías, y generar resúmenes semanales con insights accionables.

## Características principales
- **Single-Activity con Navigation Compose:** flujo que arranca en onboarding o Home según preferencias almacenadas. Navegación hacia resumen semanal, historial y ajustes desde la barra inferior.
- **Registro guiado de decisiones:** asistente en pasos con validación, selección de emociones, intensidad y categoría, incluyendo edición y borrado.
- **Home orientado al día:** lista reactiva de decisiones por fecha, filtros por búsqueda y categoría, y control de diseño de tarjetas.
- **Resúmenes e histórico semanales:** cálculo de métricas (tono calmado/impulsivo, distribución por categorías, highlights e insights) y exportación a PDF desde el historial.
- **Preferencias y recordatorios:** DataStore para nombre, inicio de semana, layout de tarjetas y fecha de primer uso; ajustes para activar recordatorios diarios (AlarmManager/WorkManager) y semanales.

## Requisitos
- Android Studio Giraffe o superior.
- JDK 11.
- Android SDK 26+ (targetSdk 36).

## Instalación y ejecución
1. Clona el repositorio y abre el proyecto en Android Studio.
2. Sincroniza gradle para descargar dependencias.
3. Ejecuta la app en un emulador o dispositivo con Android 8.0 (API 26) o superior.
4. Para compilar desde consola:
   ```bash
   ./gradlew :app:assembleDebug
   ```

## Estructura del proyecto
- `app/src/main/java/com/dmood/app`
  - `DmoodApplication.kt`: inicializa el service locator y sincroniza recordatorios al arrancar.
  - `MainActivity.kt`: punto de entrada Compose y definición de la navegación principal.
  - `data/`: Room (database, dao, entity), DataStore de preferencias y repositorios.
  - `domain/`: modelos de dominio y casos de uso (validación, tono, cálculo semanal, insights).
  - `reminder/`: scheduler, workers y helper de notificaciones para recordatorios diarios y semanales.
  - `ui/`: temas, componentes, navegación y pantallas (Home, DecisionEditor, Onboarding, Summary, History, Settings, FAQ) con sus ViewModels.
- `app/src/main/res`: recursos de iconos, temas y XML de backup/data extraction.
- `docs/`: especificaciones originales y reglas de insights.

## Uso básico
1. **Onboarding:** la primera ejecución muestra el onboarding; al completarlo se marca en DataStore para saltarlo en aperturas futuras.
2. **Home diario:** crea decisiones del día, cambia de fecha y filtra por texto/categoría. Activa el modo borrado múltiple para limpiar decisiones de hoy.
3. **Editor de decisión:** completa los pasos (texto → emociones/intensidad → categoría/confirmación). Se valida el contenido y se calcula el tono antes de guardar.
4. **Resumen semanal:** accede a la pestaña Resumen; si aún no hay ventana elegible se muestra la próxima fecha. Cuando hay datos se generan métricas, highlights e insights.
5. **Histórico y exportación:** desde Ajustes abre el histórico semanal, revisa semanas anteriores y exporta cada resumen a PDF.
6. **Ajustes y recordatorios:** edita el nombre, cambia el día de inicio de semana (con límite mensual) y activa/desactiva recordatorios diarios/semanales.

## Notas sobre recordatorios
- **Recordatorio diario:** se programa con `AlarmManager` a las 21:00 por defecto; si no es posible, se usa un fallback con `WorkManager` de una sola ejecución.
- **Recordatorio semanal:** se agenda con `WorkManager` cada 24h para verificar si toca mostrar un nuevo resumen.
- **Sincronización:** al iniciar la app y al cambiar preferencias se sincronizan o cancelan ambos recordatorios según corresponda.

