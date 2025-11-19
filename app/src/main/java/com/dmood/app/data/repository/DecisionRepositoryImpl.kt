package com.dmood.app.data.repository

import com.dmood.app.data.local.dao.DecisionDao
import com.dmood.app.data.mapper.DecisionMapper
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.repository.DecisionRepository

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
        return dao.getAll().map { DecisionMapper.toDomain(it) }
    }

    override suspend fun getByDay(timestamp: Long): List<Decision> {
        return dao.getByDay(timestamp).map { DecisionMapper.toDomain(it) }
    }

    override suspend fun getByRange(start: Long, end: Long): List<Decision> {
        return dao.getByRange(start, end).map { DecisionMapper.toDomain(it) }
    }
}
