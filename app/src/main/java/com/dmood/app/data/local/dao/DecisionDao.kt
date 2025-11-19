package com.dmood.app.data.local.dao

import androidx.room.*
import com.dmood.app.data.local.entity.DecisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(decision: DecisionEntity): Long

    @Update
    suspend fun update(decision: DecisionEntity)

    @Delete
    suspend fun delete(decision: DecisionEntity)

    /**
     * Flujo reactivo del día (Home)
     */
    @Query(
        """
        SELECT * FROM decisions
        WHERE timestamp >= :startMillis
          AND timestamp < :endMillis
        ORDER BY timestamp ASC
        """
    )
    fun getDecisionsForDay(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<DecisionEntity>>

    /**
     * Resumen semanal (One-shot)
     */
    @Query(
        """
        SELECT * FROM decisions
        WHERE timestamp >= :startMillis
          AND timestamp < :endMillis
        ORDER BY timestamp ASC
        """
    )
    suspend fun getDecisionsForRange(
        startMillis: Long,
        endMillis: Long
    ): List<DecisionEntity>

    @Query("SELECT * FROM decisions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DecisionEntity?
}
