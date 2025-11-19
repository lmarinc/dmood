package com.dmood.app.data.mapper

import com.dmood.app.data.local.entity.DecisionEntity
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.model.DecisionTone
import com.dmood.app.domain.model.EmotionType

/**
 * Mapper entre el modelo de dominio [Decision] y la entidad Room [DecisionEntity].
 *
 * Emociones se serializan como una lista de nombres de enum separados por comas.
 */
object DecisionMapper {

    /**
     * Convierte una [Decision] de dominio a [DecisionEntity] para persistencia local.
     */
    fun toEntity(model: Decision): DecisionEntity {
        val emotionsString = model.emotions.joinToString(separator = ",") { it.name }

        return DecisionEntity(
            id = model.id,
            timestamp = model.timestamp,
            text = model.text,
            emotionsJson = emotionsString,
            intensity = model.intensity,
            category = model.category.name,
            tone = model.tone.name
        )
    }

    /**
     * Convierte una [DecisionEntity] almacenada en base de datos a [Decision] de dominio.
     */
    fun toDomain(entity: DecisionEntity): Decision {
        val emotions: List<EmotionType> =
            if (entity.emotionsJson.isBlank()) {
                emptyList()
            } else {
                entity.emotionsJson
                    .split(",")
                    .mapNotNull { raw ->
                        raw.trim().takeIf { it.isNotEmpty() }?.let {
                            runCatching { EmotionType.valueOf(it) }.getOrNull()
                        }
                    }
            }

        return Decision(
            id = entity.id,
            timestamp = entity.timestamp,
            text = entity.text,
            emotions = emotions,
            intensity = entity.intensity,
            category = CategoryType.valueOf(entity.category),
            tone = DecisionTone.valueOf(entity.tone)
        )
    }
}