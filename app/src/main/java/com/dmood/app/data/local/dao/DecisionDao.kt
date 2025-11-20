package com.dmood.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
     * Resumen / consultas por rango (one-shot)
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

    // Nuevo: primer timestamp disponible
    @Query("SELECT MIN(timestamp) FROM decisions")
    suspend fun getEarliestTimestamp(): Long?
}
