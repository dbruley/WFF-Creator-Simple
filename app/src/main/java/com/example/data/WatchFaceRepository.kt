package com.example.data

import android.content.Context
import com.example.model.WatchFaceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WatchFaceRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.watchFaceDao()

    val allWatchFaces: Flow<List<WatchFaceEntity>> = dao.getAllWatchFaces()

    suspend fun initializePresetsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllWatchFaces().first()
        if (existing.isEmpty()) {
            DefaultPresets.presets.forEach { preset ->
                val json = WatchFaceJsonHelper.toJson(preset)
                dao.insertWatchFace(
                    WatchFaceEntity(
                        name = preset.name,
                        targetWearOsVersion = preset.targetWearOsVersion,
                        clockType = preset.clockType.name,
                        themeColor = preset.themeColor,
                        configJson = json,
                        isFavorite = preset.id == "preset_chrono_velocity"
                    )
                )
            }
        }
    }

    suspend fun saveWatchFace(config: WatchFaceConfig, entityId: Long? = null): Long = withContext(Dispatchers.IO) {
        val json = WatchFaceJsonHelper.toJson(config)
        val entity = WatchFaceEntity(
            id = entityId ?: 0,
            name = config.name,
            targetWearOsVersion = config.targetWearOsVersion,
            clockType = config.clockType.name,
            themeColor = config.themeColor,
            configJson = json,
            updatedAt = System.currentTimeMillis()
        )
        if (entityId != null && entityId > 0) {
            dao.updateWatchFace(entity)
            entityId
        } else {
            dao.insertWatchFace(entity)
        }
    }

    suspend fun deleteWatchFace(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(id, isFavorite)
    }

    suspend fun loadConfig(id: Long): WatchFaceConfig? = withContext(Dispatchers.IO) {
        val entity = dao.getWatchFaceById(id) ?: return@withContext null
        WatchFaceJsonHelper.fromJson(entity.configJson)
    }
}
