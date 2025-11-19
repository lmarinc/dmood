package com.dmood.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val text: String,
    val emotionsJson: String,
    val intensity: Int,
    val category: String,
    val tone: String
)
