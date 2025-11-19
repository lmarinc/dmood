package com.dmood.app.data.local.dao

import androidx.room.*
import androidx.room.Insert
import com.dmood.app.data.local.entity.DecisionEntity

@Dao
interface DecisionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(decision: DecisionEntity): Long

    @Update
    suspend fun update(decision: DecisionEntity)

    @Delete
    suspend fun delete(decision: DecisionEntity)

    @Query("SELECT * FROM decisions ORDER BY timestamp DESC")
    suspend fun getAll(): List<DecisionEntity>

    @Query("SELECT * FROM decisions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getByRange(start: Long, end: Long): List<DecisionEntity>

    @Query("SELECT * FROM decisions WHERE date(timestamp / 1000, 'unixepoch') = date(:day / 1000, 'unixepoch')")
    suspend fun getByDay(day: Long): List<DecisionEntity>
}
