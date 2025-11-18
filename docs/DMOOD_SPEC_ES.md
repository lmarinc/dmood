BEGIN PROMPT

[ROL DEL MODELO]

Actúa como un DESARROLLADOR ANDROID SENIOR especializado en:

Kotlin

Jetpack Compose

Material 3

Room

Navegación con Navigation Compose

Arquitectura MVVM + capas data/domain/ui (clean pero ligera, sin overengineering)

Tu tarea es GENERAR TODO EL CÓDIGO necesario para una app Android llamada “D-Mood”, basada en las especificaciones que detallo a continuación.

Quiero que escribas el código como si fueras a montar un proyecto Android Studio completo y funcional, con buena estructura y listo para escalar.

IMPORTANTE:

Sigue al pie de la letra los requisitos funcionales y de UX que describo.

No simplifiques la lógica de negocio ni las reglas emocionales.

No inventes nuevas features fuera del alcance, pero sí puedes crear pequeñas utilidades si ayudan a la organización del código.

Organiza el código en paquetes y ficheros de forma limpia y coherente.

==================================================
BLOQUE 1 / 9 – CONTEXTO GENERAL Y VISIÓN DEL PRODUCTO

Nombre de la app: D-Mood

Descripción corta:
D-Mood es un micro diario de decisiones y emociones. El usuario registra decisiones puntuales de su día, anota cómo se sentía cuando las tomó y en qué ámbito de su vida encajan. La app genera un resumen semanal tipo Wrapped, destacando patrones de comportamiento y tono emocional, sin ser terapia ni diagnóstico.

Puntos clave:

No hay login.

No hay sincronización en la nube.

Todo se guarda localmente en el dispositivo con Room.

D-Mood no es una app clínica, sino una herramienta de autoconomiento.

El foco es la simplicidad de uso, UX muy clara, mínima fricción.

Flujo conceptual:

El usuario abre la app.

La primera vez ve el onboarding (4 pantallas).

Después entra siempre en “Hoy”:

Puede registrar decisiones de ese día.

Puede marcar una emoción general del día.

Desde la barra inferior puede ir:

a “Resumen” (Wrapped semanal),

a “Ajustes”,

y siempre volver a “Hoy”.

Una vez por semana puede ver un resumen tipo Wrapped con:

ritmo de decisiones,

emociones más presentes,

ámbitos principales,

proporción calmadas/impulsivas (interno),

highlights (“Lo más destacable”) uno por pantalla.

==================================================
BLOQUE 2 / 9 – ARQUITECTURA TÉCNICA Y ESTRUCTURA DE PROYECTO

Tecnologías:

Kotlin

Jetpack Compose

Material 3

Room

Navigation Compose

Arquitectura:

Single-Activity app con Navigation Compose.

Patrón MVVM:

Capa data (Room, repos, mappers).

Capa domain (modelos puros + casos de uso).

Capa ui (ViewModels + pantallas Compose).

Estructura de paquetes (ejemplo):

package com.dmood.app

data

local

AppDatabase.kt

DecisionEntity.kt

DailyMoodEntity.kt

DecisionDao.kt

DailyMoodDao.kt

Converters.kt

repository

DecisionRepository.kt

DecisionRepositoryImpl.kt

domain

model

EmotionType.kt

CategoryType.kt

DecisionTone.kt

Decision.kt

DailyMood.kt

WeeklySummary.kt

WeeklyHighlight.kt

usecase

CalculateDecisionToneUseCase.kt

BuildWeeklySummaryUseCase.kt

ui

navigation

NavRoutes.kt

AppNavHost.kt

theme

Color.kt

Theme.kt

Type.kt (si quieres)

onboarding

OnboardingViewModel.kt

OnboardingScreen.kt

home

HomeViewModel.kt

HomeScreen.kt

decision

DecisionStepperViewModel.kt

DecisionStepperScreen.kt

guide

GuideScreen.kt

settings

SettingsScreen.kt

summary

WeeklySummaryViewModel.kt

WeeklySummaryScreen.kt

Puedes ajustar nombres, pero mantén esta lógica de separación.

==================================================
BLOQUE 3 / 9 – MODELO DE DOMINIO Y REGLAS EMOCIONALES

3.1. Emociones – EmotionType

Define un enum EmotionType con estos valores (usa nombres de enum en inglés, pero asegúrate de tener displayName o similar en español):

ALEGRE

SEGURO

MIEDO

SORPRENDIDO

TRISTE

INCOMODO

ENFADADO

MOTIVADO

NORMAL

Necesidades:

Cada EmotionType debe tener:

displayName: String (ej: “Alegre”, “Incómodo”)

color asociado (Color de Compose) para la UI (tonos suaves).

Define funciones/helpers para saber si una emoción es positiva o negativa.

Grupos:

Positivas: ALEGRE, SEGURO, MOTIVADO, SORPRENDIDO

Negativas: TRISTE, MIEDO, ENFADADO, INCOMODO

NORMAL se considera base, no entra en positivo/negativo.

Reglas de selección para una decisión:

Máximo 2 emociones simultáneas.

Si se selecciona NORMAL:

Se deseleccionan todas las demás.

Solo puede haber NORMAL.

Si se seleccionan dos emociones y son de signo opuesto (una positiva y otra negativa), hay que mostrar un aviso (Snackbar o texto informativo) explicando que es un estado complejo/ambivalente, pero se permite.

3.2. Categorías – CategoryType

Define enum CategoryType con:

TRABAJO_ESTUDIOS (display "Trabajo/Estudios")

SALUD_BIENESTAR (display "Salud/Bienestar")

RELACIONES_SOCIAL (display "Relaciones/Social")

FINANZAS_COMPRAS (display "Finanzas/Compras")

HABITOS_CRECIMIENTO (display "Hábitos/Crecimiento")

OCIO_TIEMPO_LIBRE (display "Ocio/Tiempo libre")

CASA_ORGANIZACION (display "Casa/Organización")

OTRO (display "Otro")

Cada CategoryType puede tener:

displayName: String.

icono opcional (String o referencia) para el futuro.

3.3. Tono de decisión – DecisionTone

Enum:

CALMADA

IMPULSIVA

NEUTRA

Reglas para calcular el tono:

Entrada:

intensidad (1..5)

lista de emociones (List<EmotionType>)

Calcular:

Es CALMADA si:

intensidad es 1 o 2

todas las emociones son positivas (ALEGRE, SEGURO, MOTIVADO, SORPRENDIDO)

no hay emociones negativas

si solo hay NORMAL, NO es CALMADA, entra en NEUTRA

Es IMPULSIVA si:

intensidad es 4 o 5

hay al menos una emoción negativa (TRISTE, MIEDO, ENFADADO, INCOMODO)

NORMAL nunca cuenta como negativa ni positiva

En cualquier otro caso es NEUTRA:

intensidades 3

intensidades bajas con mezcla positiva/negativa

decisiones con NORMAL (solo o combinaciones permitidas, si en algún momento se permitiesen, pero en esta versión Normal es exclusiva).

Implementa una función pura en domain:
fun calculateDecisionTone(intensity: Int, emotions: List<EmotionType>): DecisionTone

3.4. Entidades de dominio

Crea las data classes:

Decision:

id: Long

timestamp: Long (millis desde epoch)

text: String

emotions: List<EmotionType>

intensity: Int (1..5)

category: CategoryType

tone: DecisionTone

DailyMood:

id: Long

date: String (formato "yyyy-MM-dd" para simplificar)

emotion: EmotionType? (puede ser null si no marca nada ese día)

WeeklySummary:

weekStart: LocalDate (o String, pero preferible LocalDate)

weekEnd: LocalDate

decisionsByDay: Map<DayOfWeek, Int>

topEmotions: List<EmotionType> (2-3 emociones más frecuentes)

categoryDistribution: Map<CategoryType, Double> (0.0..1.0)

calmadasPercentage: Double? (0.0..1.0, puede ser null si no hay suficientes decisiones clasificables)

impulsivasPercentage: Double? (0.0..1.0, puede ser null)

highlights: List<WeeklyHighlight>

WeeklyHighlight:

title: String

bigNumberLabel: String? (ejemplo "≈ 40%")

description: String

context: String? (línea de contexto adicional, opcional)

==================================================
BLOQUE 4 / 9 – CAPA DATA: ROOM, DAOs Y REPOSITORIO

4.1. Entidades Room

DecisionEntity:

id: Long (PrimaryKey, autogenerada)

timestamp: Long

text: String

emotionsRaw: String (ejemplo “ALEGRE,TRISTE”)

intensity: Int

categoryRaw: String

toneRaw: String

DailyMoodEntity:

id: Long (PrimaryKey, autogenerada)

date: String ("yyyy-MM-dd", única por día)

emotionRaw: String? (puede ser null)

Crea TypeConverters:

EmotionType ↔ String

List<EmotionType> ↔ String

CategoryType ↔ String

DecisionTone ↔ String
Si quieres, puedes almacenar directamente EmotionTypeName y luego parsear, o bien usar JSON simple. Manténlo fácil.

4.2. AppDatabase

@Database(
entities = [DecisionEntity, DailyMoodEntity],
version = 1,
exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
abstract fun decisionDao(): DecisionDao
abstract fun dailyMoodDao(): DailyMoodDao
}

4.3. DAOs

DecisionDao:

suspend fun insertDecision(entity: DecisionEntity): Long

suspend fun updateDecision(entity: DecisionEntity)

suspend fun deleteDecision(entity: DecisionEntity)

fun getDecisionsForDay(startMillis: Long, endMillis: Long): Flow<List<DecisionEntity>>

suspend fun getDecisionsForWeek(startMillis: Long, endMillis: Long): List<DecisionEntity>

fun getAllDecisions(): Flow<List<DecisionEntity>> (opcional)

DailyMoodDao:

suspend fun upsertDailyMood(entity: DailyMoodEntity) (insert or replace por date)

fun getDailyMoodForDate(date: String): Flow<DailyMoodEntity?>

suspend fun getDailyMoodsForWeek(startDate: String, endDate: String): List<DailyMoodEntity>

4.4. Repositorio

Crea interfaz DecisionRepository con métodos:

fun getTodayDecisionsFlow(todayStartMillis: Long, todayEndMillis: Long): Flow<List<Decision>>

suspend fun addOrUpdateDecision(decision: Decision)

suspend fun deleteDecision(decisionId: Long)

suspend fun getWeeklyDecisions(weekStartMillis: Long, weekEndMillis: Long): List<Decision>

fun getDailyMoodFlow(date: String): Flow<DailyMood?>

suspend fun setDailyMood(date: String, emotion: EmotionType?)

suspend fun getWeeklyDailyMoods(weekStartDate: String, weekEndDate: String): List<DailyMood>

Implementación DecisionRepositoryImpl:

Mapea Entities <-> Domain.

En addOrUpdateDecision:

Calcula tone = calculateDecisionTone(intensity, emotions) antes de guardar.

En setDailyMood:

Si emotion es null, puedes borrar el registro o registrar null según decidas, pero lo más simple es upsert con emotionRaw null.

==================================================
BLOQUE 5 / 9 – CASOS DE USO Y LÓGICA DEL RESUMEN SEMANAL

5.1. CalculateDecisionToneUseCase

Use case (o función pura) que encapsula la lógica de tono.

Entrada: intensity (1..5), emotions (List<EmotionType>)

Salida: DecisionTone

Implementa las reglas definidas en el Bloque 3.

5.2. BuildWeeklySummaryUseCase

Use case principal para generar el resumen semanal tipo Wrapped.

Entrada:

weekStart: LocalDate

weekEnd: LocalDate

lista de Decision de esa semana

lista de DailyMood de esa semana

Debes:

Calcular decisionsByDay:

Map<DayOfWeek, Int> con la cuenta de decisiones por día.

TopEmotions:

Contar emociones por Decision (cada emoción cuenta).

Ordenar y devolver las 2-3 más frecuentes.

CategoryDistribution:

Para cada CategoryType: porcentaje = decisionesEnCategoria / totalDecisionesSemana.

Double en 0.0..1.0.

calmadasPercentage e impulsivasPercentage:

Considera solo las decisiones que sean CALMADA o IMPULSIVA.

calmadasPercentage = calmadas / (calmadas + impulsivas).

impulsivasPercentage = impulsivas / (calmadas + impulsivas).

Si (calmadas + impulsivas) < cierto mínimo (ej 3), puedes devolver null.

Highlights:

Implementa la lógica de hallazgos (H1–H6) descrita a continuación.

Cada highlight genera un WeeklyHighlight.

Selecciona máximo 4 por semana, ordenados por prioridad.

5.3. Reglas de Highlights (Lo más destacable)

Definiciones auxiliares:

TotalDecisionesSemana = número de Decision en esa semana.

Considera porcentaje como Double (0.0..1.0) y conviértelo a texto "≈ XX%" redondeando.

H1 – Concentración en mitad de la semana (prioridad alta, conductual)

Considera semana empezando en lunes.

MidWeek = miércoles + jueves.

pctMid = (decisionesMiercoles + decisionesJueves) / total.

Condición:

pctMid >= 0.35

y pctMid al menos 0.10 por encima del promedio de otros bloques (Lunes+Martes y Viernes+Sábado+Domingo).

Si se cumple:

title: "Mayor concentración en mitad de la semana"

bigNumberLabel: "≈ X%" (X = pctMid*100 redondeado)

description: “Aproximadamente un X% de tus decisiones se han concentrado en mitad de la semana. Es donde tu actividad ha sido más densa.”

context: texto corto opcional del estilo: “El resto de días han tenido un ritmo más ligero.”

H2 – Decisiones intensas al final del día (temporal + emocional)

Definición franja noche: 19:00–02:59 (usa timestamp para deducir hora).

Considera decisiones intensas: intensity >= 4.

totalIntensas = count(decisiones con intensidad>=4)

intensasNoche = count(decisiones intensas en franja noche)

pctNoche = intensasNoche / totalIntensas

Condición:

totalIntensas >= 3

pctNoche >= 0.4

Si se cumple:

title: "Más intensidad al final del día"

bigNumberLabel: "≈ X%" (X = pctNoche*100)

description: “Aproximadamente un X% de tus decisiones más intensas se han tomado al final del día. Es el tramo donde las decisiones parecen cargar más peso.”

context: por ejemplo: “Puede ser un momento donde tu estado está más cargado o cansado.”

H3 – Bajo porcentaje de ocio/descanso (conductual + salud)

Grupo descanso = categorías OCIO_TIEMPO_LIBRE + SALUD_BIENESTAR

decDescanso = decisiones en esas categorías.

pctDescanso = decDescanso / total

Condición:

total >= 5

pctDescanso <= 0.20

Si se cumple:

title: "Descanso y ocio en segundo plano"

bigNumberLabel: "≈ X%" (X = pctDescanso*100)

description: “Solo alrededor de un X% de tus decisiones han estado ligadas a ocio o descanso. El resto se ha orientado a otras áreas.”

context: opcional, por ejemplo: “Puede ser útil revisar cuánto espacio dejas a actividades que recargan energía.”

H4 – Motivación ligada a Trabajo/Estudios (cognitivo + conductual)

totalMotivado = decisiones donde MOTIVADO ∈ emociones.

motivadoTrabajo = decisiones con MOTIVADO y categoría TRABAJO_ESTUDIOS.

pctMotivadoTrabajo = motivadoTrabajo / totalMotivado

Condición:

totalMotivado >= 3

pctMotivadoTrabajo >= 0.4

Si se cumple:

title: "Motivación orientada al trabajo/estudios"

bigNumberLabel: "≈ X%"

description: “Cerca de un X% de las decisiones tomadas con motivación han estado ligadas a Trabajo/Estudios. La energía de avanzar ha ido sobre todo hacia este ámbito.”

context: opcional.

H5 – Predominio de emociones positivas o negativas (emocional)

Para cada decisión:

marcar si tiene al menos una emoción positiva.

marcar si tiene al menos una emoción negativa.

decPos = count(decisiones con al menos una positiva)

decNeg = count(decisiones con al menos una negativa)

pctPos = decPos / total

pctNeg = decNeg / total

Condiciones:

Si pctPos - pctNeg >= 0.15 → highlight positivo.

Si pctNeg - pctPos >= 0.15 → highlight negativo.

Highlight positivo:

title: "Emociones positivas por delante"

bigNumberLabel: "≈ X%" (X = pctPos*100)

description: “Aproximadamente un X% de tus decisiones han ido acompañadas de emociones positivas como alegría, seguridad o motivación.”

context: opcional.

Highlight negativo:

title: "Más carga emocional negativa esta semana"

bigNumberLabel: "≈ X%" (X = pctNeg*100)

description: “Aproximadamente un X% de tus decisiones han ido acompañadas de emociones como tristeza, miedo, enfado o incomodidad.”

context: opcional.

H6 – Ámbito dominante (conductual)

Para cada CategoryType:

pctCat = decisionesEnCategoria / total.

Si alguna categoría tiene pctCat >= 0.3:

Elegir la de mayor pct.

title: por ejemplo: "Trabajo/Estudios como eje de la semana" (si la categoría es TRABAJO_ESTUDIOS) o genérico “Un ámbito ha concentrado buena parte de las decisiones”.

bigNumberLabel: "≈ X%" (X = pctCat*100)

description: “Aproximadamente un X% de las decisiones se han dado en {Categoría}. Es el área que más ha ocupado tu espacio de decisión.”

context: opcional.

Selección y priorización:

Generar lista de todos los highlights que cumplen condición.

Orden de prioridad:

H6 – Ámbito dominante

H1 – Mitad de semana

H3 – Bajo ocio/descanso

H5 – Predominio emociones

H2 – Intensidad noche

H4 – Motivación+Trabajo

Seleccionar máximo 4 para highlights.

Si no se cumple ningún highlight:

Crear un WeeklyHighlight genérico:

title: "Aún sin patrones claros"

bigNumberLabel: null

description: "Esta semana hay pocas decisiones registradas o muy repartidas. A medida que vayas añadiendo más, aquí aparecerán hallazgos concretos sobre tus patrones."

context: null

==================================================
BLOQUE 6 / 9 – UX Y PANTALLAS: ONBOARDING, HOME, STEPPER

6.1. Navegación general

Define rutas (sealed class o constantes):

"onboarding"

"home"

"decision_stepper"

"guide"

"settings"

"weekly_summary"

Single Activity con NavHost. Muestra onboarding solo la primera vez (puedes usar DataStore o un flag en memoria para el MVP).

6.2. Onboarding (4 pantallas)

OnboardingViewModel:

Estado: nombre, icono seleccionado, inicioSemana, flag completado.

Puedes simular guardado en DataStore (MVP: in-memory o TODO).

Pantalla 1 – Intro:

Logo simple de D-Mood.

Título: “Autoconocimiento en pequeño formato”.

Texto: “D-Mood es un micro diario de decisiones y emociones. No es terapia, pero puede ayudarte a ver patrones que influyen en tu bienestar.”

Botón: “Continuar” → Pantalla 2.

Pantalla 2 – Qué registrar:

Título: “Qué registrar”.

Bloque “Añádelo si implica decisión”:

Ejemplos:

“Elegí salir a caminar para despejarme.”

“Decidí no responder un mensaje de inmediato.”

“Opté por posponer una tarea para descansar.”

“Organicé una conversación para aclarar un tema.”

Bloque “No hace falta registrar”:

Acciones rutinarias:

“Comer por costumbre.”

“Ducharse.”

“Desplazamientos diarios fijos.”

Botón: “Continuar” → Pantalla 3.

Pantalla 3 – Personalización:

Campo nombre (opcional): “Tu nombre”.

Chips para icono personal (simples símbolos, ej. ●, ◇, △, ◎, ⋄, ~).

Chips para “¿Con qué día empieza tu semana?”: Lunes / Domingo / Sábado.

Botón: “Continuar” → Pantalla 4.

Pantalla 4 – Privacidad:

Título: “Tus datos son tuyos”.

Texto: “Todo se guarda solo en este dispositivo. No enviamos ni analizamos tus registros en servidores. Puedes borrarlos cuando quieras.”

Chip o texto destacado: “Modo local 100%”.

Botón: “Entrar a D-Mood” → marca onboarding completado y navega a Home.

6.3. Home (Hoy)

HomeViewModel:

Obtiene lista de Decision del día actual (Flow).

Obtiene DailyMood del día actual (Flow).

Expone estado: listado de decisiones, emoción general del día, etc.

UI:

TopAppBar:

Título: D-Mood

Subtítulo: “Hoy” (puede mostrar fecha formateada).

Icono “?” que navega a Guide.

Card “Emoción general del día”:

Pregunta: “¿Cómo te has sentido hoy en general?”

Chips de emociones (EmotionType) en formato simple:

Solo se permite seleccionar UNA emoción (o ninguna).

Al pulsar un chip:

Actualizar DailyMood para la fecha actual.

Texto debajo: “Hoy, en general te has sentido: {Emoción}.”

Si no hay emoción seleccionada:

Texto: “Aún no has seleccionado un estado general.”

Lista de decisiones de hoy:

Si está vacía:

Card con texto: “Aún no has registrado ninguna decisión hoy.”

Texto de ayuda: “Pulsa el botón “+” para añadir la primera.”

Si hay decisiones:

Para cada Decision:

Hora (derivada de timestamp).

Texto de la decisión.

Chips de emociones (si las hay).

Indicador de intensidad (ej. puntos ●●○○○ según valor).

Nombre de categoría.

Opcional: pequeñas acciones "Editar" y "Borrar" (MVP puede dejar Editar como TODO).

FAB (+):

Abre la pantalla DecisionStepper (nueva decisión).

Bottom Navigation con 3 ítems:

Hoy

Resumen

Ajustes

==================================================
BLOQUE 7 / 9 – UX Y PANTALLAS: STEPPER, GUÍA, AJUSTES, RESUMEN

7.1. Stepper nueva decisión (DecisionStepperScreen)

DecisionStepperViewModel:

Estado: pasoActual (1, 2, 3).

Campos: textoDecision, emocionesSeleccionadas, intensidad, categoriaSeleccionada.

Lógica para guardar decisión usando el repo.

Paso 1:

Título: “¿Qué decisión quieres registrar?”

TextField multiline para textoDecision.

Mensaje de ayuda: “Por ejemplo: “Elegí posponer una tarea para descansar”.”

Botón “Continuar”:

Si texto vacío → mostrar error (Snackbar o texto).

Si ok → pasar a paso 2.

Paso 2:

Título: “¿Cómo te sentías cuando tomaste esta decisión?”

Chips de emociones con colores:

Todas las EmotionType (ALEGRE, SEGURO, MIEDO, SORPRENDIDO, TRISTE, INCOMODO, ENFADADO, MOTIVADO, NORMAL).

Lógica:

Si el usuario pulsa NORMAL:

Si había otras emociones seleccionadas, mostrar diálogo/confirmación:

“Elegir ‘Normal’ sustituye las emociones seleccionadas. ¿Quieres continuar?”

Si acepta: deja solo NORMAL.

Para el resto:

Si NORMAL estaba seleccionado previamente: se deselecciona NORMAL.

Máximo 2 emociones:

Si intenta seleccionar una tercera → mostrar aviso: “Solo puedes seleccionar hasta 2 emociones para esta decisión.”

Si la selección incluye al menos una positiva y al menos una negativa:

Mostrar aviso: “Estás combinando emociones de signo opuesto. Esto puede reflejar un estado complejo. Si así te sentías, puedes mantenerlo.”

Slider de intensidad (1..5):

Etiqueta: “Intensidad emocional”.

Mostrar visualmente puntos o barra.

Botón “Continuar” → paso 3.

Paso 3:

Título: “¿En qué ámbito encaja?”

Chips de CategoryType.

Obligatorio elegir uno:

Si no hay selección y se pulsa “Guardar” → mostrar error: “Selecciona una categoría para continuar.”

Botón “Guardar”:

Construir Decision:

timestamp = now

tone = calculateDecisionTone(intensity, emociones)

Guardar vía repositorio.

Volver a Home.

7.2. Guide / Guía

GuideScreen:
Contenido en scroll:

Sección “¿Qué es D-Mood?”:

Explicar brevemente:

Micro diario de decisiones y emociones.

No es terapia ni diagnóstico.

Ayuda a observar patrones.

Sección “Qué registrar / qué no registrar”:

Lista simple con ejemplos ya definidos en onboarding.

Sección “Mapa emocional D-Mood”:

Describir cada EmotionType con una frase y su color aproximado:

Alegre – energía positiva, ligereza.

Seguro – confianza en ti o en la situación.

Motivado – ganas de avanzar o actuar.

Sorprendido – algo inesperado te afecta.

Triste – pérdida, bajón o sensibilidad.

Incómodo – algo no encaja o te tensa.

Enfadado – límite, injusticia o irritación.

Miedo – amenaza, riesgo o preocupación intensa.

Normal – sin carga emocional especial.

Texto explicativo:

Puedes elegir hasta 2 emociones por decisión.

Si seleccionas Normal, se usa solo esa.

Si combinas emociones muy opuestas, reflejamos que es un estado complejo, sin juicios.

Sección “¿Puede ayudar a mi salud mental?”:

Texto neutro:

D-Mood no da diagnósticos ni sustituye ayuda profesional.

Sirve para observar cómo decides y cómo te sientes, lo cual puede apoyar tu autoconomiento.

Sección “Privacidad”:

Recalcar:

Todo se guarda solo en tu dispositivo.

No enviamos datos a servidores.

7.3. Ajustes / Settings

SettingsScreen:

Sección “Tu perfil local”:

Mostrar nombre configurado (solo lectura en V1).

Mostrar icono elegido.

Mostrar día de inicio de semana.

Sección “D-Mood Pro” (informativa, sin billing todavía):

Explicar que en el futuro Pro podría incluir:

Resúmenes mensuales.

Historial de semanas.

Patrones a largo plazo.

Mostrar tarjetas con precios demo (ej. 0,99 €/mes) sin funcionalidad.

Botón: “Ver guía y ayuda” → navega a GuideScreen.

7.4. Resumen semanal / WeeklySummaryScreen (tipo Wrapped)

WeeklySummaryViewModel:

Determina semana actual según día de inicio de semana.

Llama al repositorio para obtener:

decisiones de la semana

daily moods de la semana

Llama a BuildWeeklySummaryUseCase.

UI:

Implementar un carrusel de slides (puede ser HorizontalPager si usas accompanist o tu propia lógica con índice y botones “Siguiente / Anterior”).

Orden de slides:

Intro:

Título: “Tu semana en decisiones”.

Texto breve explicando que se resumen tendencias, no juicios.

Ritmo de decisiones:

Mostrar días Lu–Do como barras sin números, solo alturas relativas.

Texto que comente visualmente si hubo días más cargados.

Emociones más presentes:

Mostrar chips o badges con topEmotions.

Texto opcional: si daily moods indican una emoción general dominante, mencionarla.

Ámbitos principales:

Mostrar 2–3 categorías con mayor porcentaje de categoryDistribution.

Mostrar approx “≈ X%” en texto.

Tono de decisiones:

Mostrar calmadasPercentage e impulsivasPercentage si no son null:

Ejemplo: “Decisiones calmadas ≈ 60%, impulsivas ≈ 40%”.
6–9) Lo más destacable:

Cada WeeklyHighlight se muestra en una pantalla independiente:

title en grande

bigNumberLabel en tamaño grande (si no es null)

description

context en texto más pequeño si lo hay.

Cierre:

Texto:

“Esta fotografía no es un juicio, solo una forma de ver tu patrón para que puedas entenderte mejor con el tiempo.”

==================================================
BLOQUE 8 / 9 – TEMA, ESTILO VISUAL Y CHECKLIST DE ENTREGA

8.1. Tema y Material 3

Crea un tema Compose con Material 3:

Paleta base modo claro:

primary = verde ≈ #4F8A6E

secondary / accent = naranja ≈ #F29C5B

background = beige claro ≈ #F7F4EF

Modo oscuro:

Deriva automáticamente usando darkColorScheme con tonos coherentes.

Define colores por emoción en el tema o en EmotionType:

Ejemplo aproximado:

ALEGRE – verde amarillento suave

SEGURO – verde medio

MIEDO – verde oscuro / azul petróleo

SORPRENDIDO – azul celeste

TRISTE – azul/violeta

INCOMODO – malva/lavanda

ENFADADO – rojo suave

MOTIVADO – naranja cálido

NORMAL – gris claro

8.2. Estilo de componentes

Usar Surface, Card, Text, Button, OutlinedTextField, etc. de Material 3.

Chips:

Usa contenedores tipo AssistChip / FilterChip / custom chip simple redondeado.

Emochips:

fondo blanco o muy claro cuando no seleccionados.

fondo del color asociado cuando seleccionados.

FAB:

Standard FAB en Home con icono “+”.

8.3. Checklist de lo que debes generar en el código

Estructura de paquetes y ficheros base.

Entidades de dominio:

EmotionType, CategoryType, DecisionTone, Decision, DailyMood, WeeklySummary, WeeklyHighlight.

Lógica de calculateDecisionTone.

Entidades Room, Converters, DAOs, AppDatabase.

Repositorio DecisionRepository y su implementación.

Use case BuildWeeklySummaryUseCase con:

cálculo de decisiones por día,

distribución de emociones,

distribución de categorías,

porcentaje calmadas/impulsivas,

generación de WeeklyHighlight según reglas H1..H6.

Navegación:

NavHost con rutas onboarding, home, decision_stepper, guide, settings, weekly_summary.

OnboardingScreen + ViewModel:

4 pasos tal y como se describe.

HomeScreen + HomeViewModel:

Emoción general del día.

Lista de decisiones del día.

FAB para nueva decisión.

DecisionStepperScreen + ViewModel:

3 pasos, con lógica de emociones (Normal exclusiva, máximo 2, combinación opuestos con aviso).

GuideScreen:

Contenidos explicativos según lo descrito.

SettingsScreen:

Perfil local, info Pro, acceso a Guía.

WeeklySummaryScreen + ViewModel:

Slides tipo Wrapped, incluyendo Lo más destacable 1 highlight por pantalla.

Theme Material 3 con la paleta definida.

Cualquier utilidad adicional necesaria (formatos de fecha, formateo de porcentajes, etc.).




BLOQUE 9 / 9 – MODO DESARROLLADOR Y SIMULACIÓN DEL PLAN PRO

Objetivo:
Poder probar la app en modo FREE y en modo PRO sin depender de Google Play Billing ni de cuenta de desarrollador, pero dejando la arquitectura preparada para integrar billing real más adelante.

Enfoque:
Se implementará un “modo desarrollador” accesible solo en builds de debug, con un toggle para simular que el usuario es Pro. En builds de release (las que se suban a Google Play) esta sección no existirá.

9.1. Flags internos

Se usarán estos flags lógicos:

devModeEnabled: Boolean – indica si el modo desarrollador está activado (solo relevante en debug).

mockProEnabled: Boolean – indica si se está simulando un usuario Pro.

Persistencia:

Ambos flags pueden guardarse en DataStore (o SharedPreferences en un MVP) para no perderse entre aperturas.

9.2. Condición central de estado Pro

Más adelante, cuando haya billing, el estado “es Pro” se calculará así:

isProUser = realBillingStatus || (BuildConfig.DEBUG && mockProEnabled)

Donde:

realBillingStatus = resultado real de Google Play Billing (true si tiene suscripción Pro activa).

En esta primera versión, realBillingStatus será siempre false (no hay billing todavía).

BuildConfig.DEBUG = true solo en builds de desarrollo.

Para esta primera versión (sin billing):

isProUser = BuildConfig.DEBUG && mockProEnabled

9.3. UI del modo desarrollador

Ubicación:

Pantalla de Ajustes (SettingsScreen).

Comportamiento:

Si BuildConfig.DEBUG == true:

En la parte inferior de Ajustes mostrar una sección adicional:

Título: “Modo desarrollador”.

Switch: “Activar modo desarrollador”.

Switch: “Simular usuario Pro” (solo interactivo si “Modo desarrollador” está activado).

Si BuildConfig.DEBUG == false:

No mostrar esta sección en absoluto.

No es necesario ocultar la opción con gestos secretos; el simple hecho de que solo exista en debug ya es suficiente para que no aparezca en producción.

9.4. Uso del flag Pro en la app

Donde sea relevante (por ejemplo, en la pantalla de Resumen o en futuras features Pro), se usará isProUser para:

Mostrar u ocultar contenido Pro.

Mostrar copy del tipo:

Si isProUser == false: “Hazte Pro para ver resúmenes mensuales, patrones a largo plazo, etc.”

Si isProUser == true: mostrar funcionalidad Pro como habilitada.

En esta V1:

El resumen semanal es accesible en versión Free.

El resumen mensual y funcionalidades avanzadas se plantean como futuras, pero la infraestructura isProUser ya estará lista para usarse.

9.5. Testing de escenarios

Gracias al modo desarrollador, el desarrollador podrá:

Ver cómo funciona la app en modo FREE:

devModeEnabled = false (o mockProEnabled = false).

Ver cómo funciona en modo PRO simulado:

devModeEnabled = true y mockProEnabled = true.

Cambiar de un estado a otro sin reinstalar la app ni depender de Google Play.



BLOQUE X — Sistema de Notificaciones y Permisos
10.1. Objetivo

El sistema de notificaciones permite a D-Mood mantener al usuario informado y acompañado en su proceso de registro emocional y toma de decisiones.
Las notificaciones tienen dos funciones principales:

Recordatorio diario cuando el usuario no ha registrado ninguna decisión.

Aviso semanal indicando que su resumen está listo (solo en modo Pro).

El diseño busca ser respetuoso, minimalista y no intrusivo.

10.2. Permiso requerido

En Android 13+ es necesario solicitar:

android.permission.POST_NOTIFICATIONS


En versiones anteriores, la app mostrará notificaciones sin necesidad de pedir permiso.

El permiso únicamente se solicitará cuando el usuario active por primera vez alguna función que requiera enviar notificaciones.

10.3. Recordatorio diario

Condición activación:
El usuario habilita el interruptor “Recordatorio diario si no hay decisiones”.

Comportamiento:

Cada día, a una hora predefinida (ej. 21:00), la app comprueba si se ha registrado alguna decisión.

Si no existe ninguna:

Se envía una notificación suave:
“Aún no has registrado ninguna decisión hoy. ¿Quieres añadir una ahora?”

La característica puede desactivarse en Ajustes en cualquier momento.

Permiso:

Si el usuario activa la opción y el permiso está pendiente, la app mostrará un diálogo explicativo y solicitará el permiso.

Si el usuario lo deniega, la opción se desactivará automáticamente.

10.4. Recordatorio semanal del resumen (Modo Pro)

Condición activación:
El usuario activa la opción “Recordatorio semanal del resumen”.

Comportamiento:

Al finalizar la semana según el día configurado por el usuario como inicio, el sistema genera el resumen semanal.

La app enviará una notificación tipo:
“Tu resumen semanal está listo. Tócalo para revisarlo.”

Permiso:

Si este modo está activado y el permiso no está concedido, la app solicitará el permiso.

Si el usuario lo deniega, el interruptor se desactivará.

10.5. Integración en Ajustes

El apartado Ajustes incluirá una sección llamada Recordatorios con:

Switch:
“Recordatorio diario si no hay decisiones”

Switch:
“Recordatorio semanal del resumen (Pro)”

Botón opcional:
“Probar notificación”
para validar manualmente el permiso y el canal de notificaciones.

La interfaz debe reflejar claramente si el permiso está concedido, denegado o pendiente.

10.6. Implementación técnica
   10.6.1 Permisos

Se usarán los contratos modernos de permisos:
ActivityResultContracts.RequestPermission.

10.6.2 Persistencia

DataStore almacenará:

si los recordatorios están activos

si el permiso ha sido concedido o denegado previamente

10.6.3 Programación de recordatorios

Versión MVP:

Uso de WorkManager para tareas diarias/semanales simples.

En caso de simplificación:
permitir ejecutar notificaciones manualmente para validar comportamiento.

10.6.4 Canal de notificación

Crear un canal estable llamado:

dmood_reminders


para Android 8+.

10.7. Experiencia de usuario

Las notificaciones deben ser breves, neutrales y no culpabilizadoras.

El usuario debe mantener control total:
activar, desactivar o revocar permisos en cualquier momento.

El sistema nunca debe enviar notificaciones si el usuario no ha activado la función voluntariamente.

Fin del Bloque X


FIN DEL REQUISITO:
Genera el código de la app D-Mood siguiendo todo lo anterior con el máximo rigor posible, sin omitir ninguna de las reglas clave descritas.

END PROMPT