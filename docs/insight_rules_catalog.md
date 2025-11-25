# Catálogo de reglas de insights

Este documento resume todas las reglas que actualmente se evalúan al generar el resumen semanal. Cada regla puede disparar diferentes combinaciones de categorías, emociones, tonos e intensidades (más de 200 escenarios posibles al cruzar los parámetros).

## Reglas globales
- **Emoción dominante global**: si la emoción más frecuente supera el 60% del total de decisiones.
- **Decisiones impulsivas**: si ≥55% de las decisiones son de tono impulsivo.
- **Decisiones calmadas**: si ≥60% de las decisiones son calmadas.
- **Impulsos con emociones negativas**: si ≥60% de las decisiones impulsivas incluyen al menos una emoción negativa.
- **Intensidad alta**: si la intensidad media semanal es ≥4.2/5.
- **Paleta emocional limitada**: si se usaron 3 o menos emociones en toda la semana.
- **Decisiones concentradas al final**: si ≥50% de las decisiones ocurrieron en las últimas 48 horas.
- **Día más retador**: detecta el día con clima emocional negativo y su emoción predominante.

## Reglas por categoría
- **Emoción dominante por categoría**: para cada `CategoryType`, si una emoción representa ≥70% de las decisiones de esa categoría y hay al menos 3 decisiones, se genera un insight específico (ej. miedo en trabajo, enfado en dinero, etc.).

## Datos de soporte utilizados
- Distribución global de emociones (`emotionDistribution`).
- Matriz categoría–emoción (`categoryEmotionMatrix`).
- Distribución de emociones por tono (`toneEmotionDistribution`).
- Conteo de decisiones por tono e intensidad.
- Distribución temporal (últimas 48h y día con clima negativo).

Estos triggers se combinan para cubrir todas las categorías, emociones y tonos disponibles, permitiendo más de 200 combinaciones de insights posibles según los datos capturados.
