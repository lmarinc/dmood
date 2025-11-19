package com.dmood.app.domain.repository

import com.dmood.app.domain.model.Decision

interface DecisionRepository {
    suspend fun add(decision: Decision): Long
    suspend fun update(decision: Decision)
    suspend fun delete(decision: Decision)
    suspend fun getAll(): List<Decision>
    suspend fun getByDay(timestamp: Long): List<Decision>
    suspend fun getByRange(start: Long, end: Long): List<Decision>
}
