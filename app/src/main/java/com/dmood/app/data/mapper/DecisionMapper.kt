package com.dmood.app.data.mapper

import com.dmood.app.data.local.entity.DecisionEntity
import com.dmood.app.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object DecisionMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun toEntity(model: Decision): DecisionEntity {
        return DecisionEntity(
            id = model.id,
            timestamp = model.timestamp,
            text = model.text,
            emotionsJson = json.encodeToString(model.emotions),
            intensity = model.intensity,
            category = model.category.name,
            tone = model.tone.name
        )
    }

    fun toDomain(entity: DecisionEntity): Decision {
        val emotions: List<EmotionType> =
            json.decodeFromString(entity.emotionsJson)

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
