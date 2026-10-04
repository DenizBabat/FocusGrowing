package com.focusgrowing.app.data.repository

import com.focusgrowing.app.data.local.dao.BackgroundDao
import com.focusgrowing.app.data.local.dao.FocusSessionDao
import com.focusgrowing.app.data.local.dao.MissionDao
import com.focusgrowing.app.data.local.dao.NotificationDao
import com.focusgrowing.app.data.local.dao.WorldDao
import com.focusgrowing.app.data.local.entity.BackgroundImageEntity
import com.focusgrowing.app.data.local.entity.DefaultBackgroundPrefEntity
import com.focusgrowing.app.data.local.entity.SubTaskEntity
import com.focusgrowing.app.data.local.entity.WorldEntity
import com.focusgrowing.app.data.mapper.toDomain
import com.focusgrowing.app.data.mapper.toEntity
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundScene
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.FocusSessionRepository
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import com.focusgrowing.app.domain.repository.WorldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val dao: MissionDao,
    private val time: TimeProvider,
) : MissionRepository {
    override fun observeMissions(): Flow<List<Mission>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeMission(id: Long): Flow<Mission?> = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getMission(id: Long): Mission? = dao.getById(id)?.toDomain()
    override suspend fun createMission(mission: Mission, subTaskTitles: List<String>): Long =
        dao.insertWithSubTasks(mission.toEntity().copy(id = 0), subTaskTitles)
    override suspend fun updateMission(mission: Mission) = dao.update(mission.toEntity())
    override suspend fun updateStatus(id: Long, status: MissionStatus, completedAt: Long?) =
        dao.updateStatus(id, status, completedAt, time.now())
    override suspend fun deleteMission(id: Long) = dao.delete(id)
    override suspend fun addSubTask(missionId: Long, title: String) {
        val position = dao.maxSubTaskPosition(missionId) + 1
        dao.insertSubTask(SubTaskEntity(missionId = missionId, title = title.trim(), isDone = false, position = position))
    }
    override suspend fun setSubTaskDone(subTaskId: Long, done: Boolean) = dao.setSubTaskDone(subTaskId, done)
    override suspend fun deleteSubTask(subTaskId: Long) = dao.deleteSubTask(subTaskId)
}

@Singleton
class FocusSessionRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao,
) : FocusSessionRepository {
    override suspend fun insert(session: FocusSession): Long = dao.insert(session.toEntity().copy(id = 0))
    override fun observeSessionsSince(fromMillis: Long): Flow<List<FocusSession>> =
        dao.observeSince(fromMillis).map { list -> list.map { it.toDomain() } }
    override fun observeCompletedFocusStartTimes(): Flow<List<Long>> = dao.observeCompletedFocusStartTimes()
    override suspend fun getCompletedFocusStartTimes(): List<Long> = dao.getCompletedFocusStartTimes()
    override fun observeCompletedFocusCount(): Flow<Int> = dao.observeCompletedFocusCount()
    override fun observeTotalFocusSeconds(): Flow<Long> = dao.observeTotalFocusSeconds()
}

@Singleton
class WorldRepositoryImpl @Inject constructor(
    private val dao: WorldDao,
    private val time: TimeProvider,
) : WorldRepository {
    private val mutex = Mutex()
    override fun observeTotalXp(): Flow<Int> = dao.observe().map { it?.totalXp ?: 0 }
    override suspend fun getTotalXp(): Int = dao.get()?.totalXp ?: 0
    override suspend fun addXp(amount: Int): Int = mutex.withLock {
        val total = (getTotalXp() + amount).coerceAtLeast(0)
        dao.upsert(WorldEntity(totalXp = total, updatedAt = time.now()))
        total
    }
}

@Singleton
class BackgroundRepositoryImpl @Inject constructor(
    private val dao: BackgroundDao,
    private val time: TimeProvider,
) : BackgroundRepository {

    override fun observeBackgrounds(): Flow<List<BackgroundImage>> =
        combine(dao.observeDefaultPrefs(), dao.observeAll()) { prefs, custom ->
            val favorites = prefs.filter { it.isFavorite }.map { it.sceneName }.toSet()
            BackgroundScene.entries.map { it.toDomain(it.name in favorites) } + custom.map { it.toDomain() }
        }

    override fun observeBackground(id: String): Flow<BackgroundImage?> {
        customDbId(id)?.let { dbId -> return dao.observeById(dbId).map { it?.toDomain() } }
        val scene = sceneOf(id) ?: return flowOf(null)
        return dao.observeDefaultPrefs().map { prefs ->
            scene.toDomain(prefs.any { it.sceneName == scene.name && it.isFavorite })
        }
    }

    override suspend fun getBackground(id: String): BackgroundImage? = observeBackground(id).first()

    override suspend fun customCount(): Int = dao.count()

    override suspend fun addCustom(uri: String, source: BackgroundSource, name: String): BackgroundImage {
        val entity = BackgroundImageEntity(
            uri = uri,
            name = name,
            source = source,
            createdAt = time.now(),
            isFavorite = false,
            alignX = 0f,
            alignY = 0f,
            zoom = 1f,
        )
        val id = dao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun delete(id: String) {
        customDbId(id)?.let { dao.delete(it) }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        val dbId = customDbId(id)
        if (dbId != null) {
            dao.setFavorite(dbId, favorite)
        } else {
            sceneOf(id)?.let { dao.upsertDefaultPref(DefaultBackgroundPrefEntity(it.name, favorite)) }
        }
    }

    override suspend fun updateAdjustment(id: String, alignX: Float, alignY: Float, zoom: Float) {
        customDbId(id)?.let {
            dao.updateAdjustment(it, alignX.coerceIn(-1f, 1f), alignY.coerceIn(-1f, 1f), zoom.coerceIn(1f, 3f))
        }
    }

    private fun customDbId(id: String): Long? =
        if (id.startsWith(BackgroundImage.CUSTOM_PREFIX)) id.removePrefix(BackgroundImage.CUSTOM_PREFIX).toLongOrNull() else null

    private fun sceneOf(id: String): BackgroundScene? =
        if (id.startsWith(BackgroundImage.DEFAULT_PREFIX)) {
            val name = id.removePrefix(BackgroundImage.DEFAULT_PREFIX)
            BackgroundScene.entries.firstOrNull { it.name == name }
        } else null
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
    private val time: TimeProvider,
) : NotificationRepository {
    override fun observeAll(): Flow<List<AppNotification>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()
    override suspend fun add(notification: AppNotification) {
        dao.insert(notification.toEntity().copy(id = 0))
        dao.deleteOlderThan(time.now() - RETENTION_MILLIS)
    }
    override suspend fun markAllRead() = dao.markAllRead()
    override suspend fun clearAll() = dao.clearAll()

    private companion object {
        const val RETENTION_MILLIS = 60L * 24 * 60 * 60 * 1000 // 60 days
    }
}
