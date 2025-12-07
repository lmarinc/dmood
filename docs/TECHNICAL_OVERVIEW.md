# Documento técnico D-Mood

## Arquitectura general
- **Patrón:** Single-Activity con Jetpack Compose y Navigation Compose, siguiendo un modelo MVVM ligero con capas data/domain/ui.
- **Inyección simple:** `DmoodServiceLocator` construye Room, repositorios, casos de uso y scheduler de recordatorios sin framework DI.
- **Persistencia:** Room para decisiones (`AppDatabase`, `DecisionDao`, `DecisionEntity`), y DataStore de preferencias para nombre, onboarding, layout, inicio de semana y recordatorios.
- **Trabajos en segundo plano:** `WorkManager` se usa para el recordatorio semanal y como fallback del recordatorio diario; `AlarmManager` agenda la alarma principal diaria.

## Componentes clave
- **UI & navegación:** `MainActivity` levanta el tema y decide destino inicial (onboarding u Home). `AppNavHost` define rutas para Onboarding, Home, Editor de decisiones, Resumen semanal, Histórico, Ajustes y FAQ.
- **ViewModels principales:**
  - `HomeViewModel`: observa decisiones del día en flujo, controla selección de fecha, filtros, layout de tarjetas y modo de borrado.
  - `DecisionEditorViewModel`: asistente multi-paso con validación de texto, selección de emociones/intensidad/categoría y guardado/edición/borrado.
  - `WeeklySummaryViewModel`: carga resumen semanal elegible según calendario, genera métricas, highlight e insights, y soporta modo demo.
  - `WeeklyHistoryViewModel`: recorre semanas elegibles, construye resúmenes históricos y permite exportar cada uno a PDF.
  - `SettingsViewModel`: gestiona nombre, cambio del inicio de semana con límite mensual y toggles de recordatorios con programación/cancelación.
- **Repositorios:** `DecisionRepositoryImpl` encapsula Room y mapea entidades a modelos de dominio; `UserPreferencesRepository` expone flujos y operaciones de DataStore, incluyendo control de cambios mensuales del inicio de semana y fecha de primer uso.
- **Casos de uso:** validación (`ValidateDecisionUseCase`), cálculo de tono (`CalculateDecisionToneUseCase`), agenda semanal (`CalculateWeeklyScheduleUseCase`), cálculo de resumen (`BuildWeeklySummaryUseCase`), highlights (`ExtractWeeklyHighlightsUseCase`) e insights (`GenerateInsightRulesUseCase`).
- **Workers y recordatorios:** `ReminderScheduler` decide entre `AlarmManager` y `WorkManager` para el recordatorio diario y agenda un trabajo periódico para el semanal. `DailyReminderWorker` y `WeeklySummaryWorker` disparan notificaciones vía `ReminderNotificationHelper`.

## Flujos principales
- **Onboarding inicial:** `MainActivity` consulta DataStore; si `hasSeenOnboarding` es falso se navega a Onboarding. Al finalizar, se navega a Home y se marca la preferencia para saltarlo en el futuro.
- **Creación/edición de decisiones:** desde Home se abre `DecisionEditorScreen`. El ViewModel valida texto y estados, calcula el tono antes de persistir en Room, y permite editar o borrar decisiones existentes.
- **Home diario:** `HomeViewModel` observa el DAO por rango del día seleccionado, permite avanzar/retroceder entre fechas válidas, filtrar por búsqueda/categoría y activar modo de borrado múltiple solo para hoy.
- **Resumen semanal:** `WeeklySummaryViewModel` consulta calendario elegible con `CalculateWeeklyScheduleUseCase`; si hay ventana disponible carga decisiones del rango y construye `WeeklySummary`, `WeeklyHighlight` e insights. Si no, muestra la próxima fecha elegible; el modo demo genera datos ficticios.
- **Histórico y exportación:** `WeeklyHistoryViewModel` itera anclas semanales desde la fecha de primer uso hacia atrás, genera resúmenes previos y permite exportar cada item a PDF en almacenamiento de descargas/caché.
- **Recordatorios:** `ReminderScheduler.syncReminders()` se llama al iniciar la app y cuando cambian preferencias. Activa/cancela la alarma diaria (21:00 por defecto con fallback) y el trabajo periódico semanal (cada 24h) para disparar notificaciones.

## Consideraciones de datos
- Las decisiones se persisten localmente; no hay sincronización remota.
- Emociones se serializan como nombres de enum separados por comas en `DecisionEntity` y se mapean de vuelta a `EmotionType` al leer.
- `UserPreferencesRepository` asegura `firstUseDate` y limita el cambio de inicio de semana a una vez cada 30 días.

## Testing y extensibilidad
- El proyecto incluye dependencias básicas de JUnit y Compose testing; no hay suites configuradas aún.
- La arquitectura por capas y el `ServiceLocator` facilitan reemplazar implementaciones en tests o introducir un framework DI en el futuro.
- Nuevas pantallas o casos de uso pueden añadirse registrando ViewModels en el factory y ampliando `AppNavHost`; nuevas preferencias deberían pasar por `UserPreferencesRepository` para mantener un único punto de verdad.
- Los recordatorios permiten ajustar fácilmente las horas de prueba en `ReminderScheduler` (constantes `DAILY_REMINDER_HOUR_TEST`/`MINUTE_TEST`).

