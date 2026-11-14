package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchFaceDao {

    @Query("SELECT * FROM watch_faces ORDER BY updatedAt DESC")
    fun getAllWatchFaces(): Flow<List<WatchFaceEntity>>

    @Query("SELECT * FROM watch_faces WHERE id = :id LIMIT 1")
    suspend fun getWatchFaceById(id: Long): WatchFaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchFace(entity: WatchFaceEntity): Long

    @Update
    suspend fun updateWatchFace(entity: WatchFaceEntity)

    @Delete
    suspend fun deleteWatchFace(entity: WatchFaceEntity)

    @Query("DELETE FROM watch_faces WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE watch_faces SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
}
