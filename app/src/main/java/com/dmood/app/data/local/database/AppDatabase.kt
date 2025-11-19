package com.dmood.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dmood.app.data.local.dao.DecisionDao
import com.dmood.app.data.local.entity.DecisionEntity

@Database(
    entities = [DecisionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun decisionDao(): DecisionDao
}
