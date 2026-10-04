package com.focusgrowing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.focusgrowing.app.data.local.entity.BackgroundImageEntity
import com.focusgrowing.app.data.local.entity.DefaultBackgroundPrefEntity
import com.focusgrowing.app.data.local.entity.FocusSessionEntity
import com.focusgrowing.app.data.local.entity.MissionEntity
import com.focusgrowing.app.data.local.entity.MissionWithSubTasks
import com.focusgrowing.app.data.local.entity.NotificationEntity
import com.focusgrowing.app.data.local.entity.SubTaskEntity
import com.focusgrowing.app.data.local.entity.WorldEntity
import com.focusgrowing.app.domain.model.MissionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Transaction
    @Query("SELECT * FROM missions WHERE status != 'ARCHIVED' ORDER BY CASE status WHEN 'IN_PROGRESS' THEN 0 WHEN 'TODO' THEN 1 ELSE 2 END, updatedAt DESC")
    fun observeAll(): Flow<List<MissionWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM missions WHERE id = :id")
    fun observeById(id: Long): Flow<MissionWithSubTasks?>

    @Transaction
    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getById(id: Long): MissionWithSubTasks?

    @Insert
    suspend fun insert(mission: MissionEntity): Long

    @Update
    suspend fun update(mission: MissionEntity)

    @Query("UPDATE missions SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MissionStatus, completedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM missions WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>)

    @Insert
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) FROM sub_tasks WHERE missionId = :missionId")
    suspend fun maxSubTaskPosition(missionId: Long): Int

    @Query("UPDATE sub_tasks SET isDone = :done WHERE id = :id")
    suspend fun setSubTaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM sub_tasks WHERE id = :id")
    suspend fun deleteSubTask(id: Long)

    @Transaction
    suspend fun insertWithSubTasks(mission: MissionEntity, titles: List<String>): Long {
        val id = insert(mission)
        if (titles.isNotEmpty()) {
            insertSubTasks(titles.mapIndexed { index, title ->
                SubTaskEntity(missionId = id, title = title, isDone = false, position = index)
            })
        }
        return id
    }
}

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :from ORDER BY startedAt ASC")
    fun observeSince(from: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT startedAt FROM focus_sessions WHERE type = 'FOCUS' AND completed = 1")
    fun observeCompletedFocusStartTimes(): Flow<List<Long>>

    @Query("SELECT startedAt FROM focus_sessions WHERE type = 'FOCUS' AND completed = 1")
    suspend fun getCompletedFocusStartTimes(): List<Long>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE type = 'FOCUS' AND completed = 1")
    fun observeCompletedFocusCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM focus_sessions WHERE type = 'FOCUS'")
    fun observeTotalFocusSeconds(): Flow<Long>
}

@Dao
interface WorldDao {
    @Query("SELECT * FROM world WHERE id = 1")
    fun observe(): Flow<WorldEntity?>

    @Query("SELECT * FROM world WHERE id = 1")
    suspend fun get(): WorldEntity?

    @Upsert
    suspend fun upsert(world: WorldEntity)
}

@Dao
interface BackgroundDao {
    @Query("SELECT * FROM background_images ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackgroundImageEntity>>

    @Query("SELECT * FROM background_images WHERE id = :id")
    fun observeById(id: Long): Flow<BackgroundImageEntity?>

    @Query("SELECT * FROM background_images WHERE id = :id")
    suspend fun getById(id: Long): BackgroundImageEntity?

    @Query("SELECT COUNT(*) FROM background_images")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: BackgroundImageEntity): Long

    @Query("DELETE FROM background_images WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE background_images SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE background_images SET alignX = :alignX, alignY = :alignY, zoom = :zoom WHERE id = :id")
    suspend fun updateAdjustment(id: Long, alignX: Float, alignY: Float, zoom: Float)

    @Query("SELECT * FROM default_background_prefs")
    fun observeDefaultPrefs(): Flow<List<DefaultBackgroundPrefEntity>>

    @Upsert
    suspend fun upsertDefaultPref(pref: DefaultBackgroundPrefEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 200")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
