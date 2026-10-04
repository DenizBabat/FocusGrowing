package com.focusgrowing.app.domain.usecase

import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.model.NotificationType
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.WorldRepository
import javax.inject.Inject

data class XpAward(val amount: Int, val newTotal: Int, val levelBefore: Int, val levelAfter: Int) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

/** Adds world XP and produces the level-up celebration + inbox entry when a new level is reached. */
class XpAwarder @Inject constructor(
    private val world: WorldRepository,
    private val notifications: NotificationRepository,
) {
    suspend fun award(amount: Int, now: Long): Pair<XpAward, Celebration.WorldLevelUp?> {
        val before = WorldProgression.levelFor(world.getTotalXp())
        val total = if (amount > 0) world.addXp(amount) else world.getTotalXp()
        val after = WorldProgression.levelFor(total)
        val award = XpAward(amount, total, before, after)
        if (!award.leveledUp) return award to null

        val state = WorldProgression.stateFor(total, completedSessions = 0)
        val unlocked = (before + 1..after).flatMap { WorldProgression.itemsUnlockedAt(it) }
        notifications.add(
            AppNotification(
                type = NotificationType.WORLD,
                title = "World Level Up!",
                message = "Your world has reached Level $after!",
                detail = if (unlocked.isEmpty()) null
                else "New elements unlocked: " + unlocked.joinToString { it.displayName },
                createdAt = now,
            ),
        )
        return award to Celebration.WorldLevelUp(
            level = after,
            xpIntoLevel = state.xpIntoLevel,
            xpForNextLevel = state.xpForNextLevel,
            unlocked = unlocked,
        )
    }
}
