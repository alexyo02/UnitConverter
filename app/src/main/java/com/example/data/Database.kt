package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversion_presets")
data class ConversionPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceUnitId: String,
    val targetUnitId: String,
    val usageCount: Int = 0
)

@Entity(tableName = "conversion_history")
data class ConversionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceUnitId: String,
    val targetUnitId: String,
    val sourceValue: Double = 0.0,
    val targetValue: Double = 0.0,
    val timestamp: Long = 0L
)

@Entity(tableName = "unit_usage")
data class UnitUsage(
    @PrimaryKey val unitId: String,
    val usageCount: Int = 0
)

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_presets ORDER BY usageCount DESC")
    fun getAllPresets(): Flow<List<ConversionPreset>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPreset(preset: ConversionPreset): Long

    @Update
    suspend fun updatePreset(preset: ConversionPreset)

    @Query("UPDATE conversion_presets SET usageCount = usageCount + 1 WHERE sourceUnitId = :sourceId AND targetUnitId = :targetId")
    suspend fun incrementUsage(sourceId: String, targetId: String)
    
    @Query("DELETE FROM conversion_presets WHERE id = :id")
    suspend fun deletePreset(id: Int)
    
    @Query("SELECT COUNT(*) FROM conversion_presets")
    suspend fun getCount(): Int

    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<ConversionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ConversionHistory)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()
    
    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteHistory(id: Int)

    @Query("SELECT * FROM unit_usage")
    fun getAllUnitUsages(): Flow<List<UnitUsage>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnitUsage(usage: UnitUsage): Long

    @Query("UPDATE unit_usage SET usageCount = usageCount + 1 WHERE unitId = :unitId")
    suspend fun incrementUnitUsage(unitId: String)
}

@Database(entities = [ConversionPreset::class, ConversionHistory::class, UnitUsage::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversionDao(): ConversionDao
}
