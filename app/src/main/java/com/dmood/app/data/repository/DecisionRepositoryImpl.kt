package com.dmood.app.data.repository

import com.dmood.app.data.local.dao.DecisionDao
import com.dmood.app.data.mapper.DecisionMapper
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.repository.DecisionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DecisionRepositoryImpl(
    private val dao: DecisionDao
) : DecisionRepository {

    override suspend fun add(decision: Decision): Long {
        return dao.insert(DecisionMapper.toEntity(decision))
    }

    override suspend fun update(decision: Decision) {
        dao.update(DecisionMapper.toEntity(decision))
    }

    override suspend fun delete(decision: Decision) {
        dao.delete(DecisionMapper.toEntity(decision))
    }

    override suspend fun getAll(): List<Decision> {
        // Usamos todo el rango posible
        return dao.getDecisionsForRange(Long.MIN_VALUE, Long.MAX_VALUE)
            .map(DecisionMapper::toDomain)
    }

    override suspend fun getByDay(timestamp: Long): List<Decision> {
        val startMillis = timestamp
        val endMillis = startMillis + 24L * 60L * 60L * 1000L
        return dao.getDecisionsForRange(startMillis, endMillis)
            .map(DecisionMapper::toDomain)
    }

    override suspend fun getByRange(start: Long, end: Long): List<Decision> {
        return dao.getDecisionsForRange(start, end)
            .map(DecisionMapper::toDomain)
    }

    override suspend fun getById(id: Long): Decision? {
        return dao.getById(id)?.let(DecisionMapper::toDomain)
    }

    override fun getDecisionsForDayFlow(
        start: Long,
        end: Long
    ): Flow<List<Decision>> {
        return dao.getDecisionsForDay(start, end)
            .map { list -> list.map(DecisionMapper::toDomain) }
    }

    override suspend fun getEarliestDecisionTimestamp(): Long? {
        return dao.getEarliestTimestamp()
    }
}
