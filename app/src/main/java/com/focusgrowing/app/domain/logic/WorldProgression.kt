package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.WorldItemType
import com.focusgrowing.app.domain.model.WorldState

/**
 * Pure rules for XP and world levels.
 * Level n → n+1 needs 100 * n XP (L2 at 100, L3 at 300, L4 at 600, L5 at 1000 ...).
 * 1 focused minute = 1 XP, so a 25 minute pomodoro gives 25 XP.
 */
object WorldProgression {
    const val MAX_LEVEL = 50

    fun xpToNextLevel(level: Int): Int = 100 * level

    fun xpForFocus(focusSeconds: Long): Int = (focusSeconds / 60).toInt().coerceAtLeast(0)

    fun missionCompletionBonus(estimatedPomodoros: Int): Int = 10 * estimatedPomodoros.coerceIn(1, 20)

    fun levelFor(totalXp: Int): Int {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        while (level < MAX_LEVEL && remaining >= xpToNextLevel(level)) {
            remaining -= xpToNextLevel(level)
            level++
        }
        return level
    }

    fun stateFor(totalXp: Int, completedSessions: Int): WorldState {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        while (level < MAX_LEVEL && remaining >= xpToNextLevel(level)) {
            remaining -= xpToNextLevel(level)
            level++
        }
        return WorldState(
            totalXp = totalXp,
            level = level,
            xpIntoLevel = remaining,
            xpForNextLevel = xpToNextLevel(level),
            completedSessions = completedSessions,
        )
    }

    /** Items visible on the island for a level. Premium items require premium. */
    fun unlockedItems(level: Int, isPremium: Boolean): Set<WorldItemType> =
        WorldItemType.entries.filter { it.unlockLevel <= level && (!it.isPremium || isPremium) }.toSet()

    fun itemsUnlockedAt(level: Int): List<WorldItemType> =
        WorldItemType.entries.filter { it.unlockLevel == level }
}
