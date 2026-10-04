package com.focusgrowing.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.focusgrowing.app.data.local.dao.BackgroundDao
import com.focusgrowing.app.data.local.dao.FocusSessionDao
import com.focusgrowing.app.data.local.dao.MissionDao
import com.focusgrowing.app.data.local.dao.NotificationDao
import com.focusgrowing.app.data.local.dao.WorldDao
import com.focusgrowing.app.data.local.entity.BackgroundImageEntity
import com.focusgrowing.app.data.local.entity.DefaultBackgroundPrefEntity
import com.focusgrowing.app.data.local.entity.FocusSessionEntity
import com.focusgrowing.app.data.local.entity.MissionEntity
import com.focusgrowing.app.data.local.entity.NotificationEntity
import com.focusgrowing.app.data.local.entity.SubTaskEntity
import com.focusgrowing.app.data.local.entity.WorldEntity

/**
 * Local-first database. When you change an entity after release, bump [version] and add a
 * Migration in DatabaseModule — never ship destructive migrations to real users.
 */
@Database(
    entities = [
        MissionEntity::class,
        SubTaskEntity::class,
        FocusSessionEntity::class,
        WorldEntity::class,
        BackgroundImageEntity::class,
        DefaultBackgroundPrefEntity::class,
        NotificationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun missionDao(): MissionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun worldDao(): WorldDao
    abstract fun backgroundDao(): BackgroundDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "focus_growing.db"
    }
}
