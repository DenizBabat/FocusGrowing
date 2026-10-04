package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.WorldItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldProgressionTest {

    @Test
    fun `levels follow the 100 x level curve`() {
        assertEquals(1, WorldProgression.levelFor(0))
        assertEquals(1, WorldProgression.levelFor(99))
        assertEquals(2, WorldProgression.levelFor(100))
        assertEquals(3, WorldProgression.levelFor(300))
        assertEquals(4, WorldProgression.levelFor(600))
        assertEquals(5, WorldProgression.levelFor(1000))
    }

    @Test
    fun `state reports xp inside the current level`() {
        val state = WorldProgression.stateFor(totalXp = 720, completedSessions = 10)
        assertEquals(4, state.level)
        assertEquals(120, state.xpIntoLevel)
        assertEquals(400, state.xpForNextLevel)
        assertEquals(0.3f, state.progress, 0.001f)
    }

    @Test
    fun `one focused minute is one xp`() {
        assertEquals(25, WorldProgression.xpForFocus(25 * 60L))
        assertEquals(0, WorldProgression.xpForFocus(59L))
    }

    @Test
    fun `premium items are hidden for free users`() {
        val free = WorldProgression.unlockedItems(level = 8, isPremium = false)
        val premium = WorldProgression.unlockedItems(level = 8, isPremium = true)
        assertFalse(WorldItemType.WINDMILL in free)
        assertTrue(WorldItemType.WINDMILL in premium)
        assertTrue(WorldItemType.HOUSE in free)
    }

    @Test
    fun `level four unlocks house, fence, garden and lake`() {
        assertEquals(
            listOf(WorldItemType.HOUSE, WorldItemType.FENCE, WorldItemType.GARDEN, WorldItemType.LAKE),
            WorldProgression.itemsUnlockedAt(4),
        )
    }
}
