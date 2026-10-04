package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.InsightEngine
import com.focusgrowing.app.domain.logic.InsightInput
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.InsightKind
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.StreakInfo
import com.focusgrowing.app.domain.usecase.MissionValidationError
import com.focusgrowing.app.domain.usecase.SaveMissionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class InsightEngineTest {
    private val zone = ZoneId.of("UTC")

    private fun session(day: Int, hour: Int, minutes: Long = 25, completed: Boolean = true) = FocusSession(
        missionId = null,
        type = SessionType.FOCUS,
        startedAt = LocalDateTime.of(2026, 9, day, hour, 0).atZone(zone).toInstant().toEpochMilli(),
        endedAt = null,
        plannedDurationSeconds = 25 * 60,
        actualDurationSeconds = minutes * 60,
        completed = completed,
        interruptionCount = 0,
        xpEarned = 0,
    )

    @Test
    fun `new users get a getting started insight`() {
        val result = InsightEngine().generate(InsightInput(listOf(session(1, 9)), emptyList(), StreakInfo.Empty, zone, Locale.US))
        assertEquals(1, result.size)
        assertEquals(InsightKind.GETTING_STARTED, result.first().kind)
    }

    @Test
    fun `morning focus is detected`() {
        val sessions = (1..5).map { session(it, 9) } + session(6, 15)
        val result = InsightEngine().generate(InsightInput(sessions, emptyList(), StreakInfo.Empty, zone, Locale.US))
        val timeOfDay = result.first { it.kind == InsightKind.TIME_OF_DAY }
        assertTrue(timeOfDay.title.contains("morning"))
    }

    @Test
    fun `short sessions rule fires under 15 minutes`() {
        val sessions = (1..4).map { session(it, 9, minutes = 10) }
        val result = InsightEngine().generate(InsightInput(sessions, emptyList(), StreakInfo.Empty, zone, Locale.US))
        assertTrue(result.any { it.id == "session_short" })
    }

    @Test
    fun `free insights are listed before premium ones`() {
        val sessions = (1..6).map { session(it, 9) }
        val result = InsightEngine().generate(InsightInput(sessions, emptyList(), StreakInfo(4, true, List(7) { true }), zone, Locale.US))
        val firstPremium = result.indexOfFirst { it.isPremium }
        val lastFree = result.indexOfLast { !it.isPremium }
        assertTrue(firstPremium == -1 || lastFree < firstPremium)
    }

    @Test
    fun `mission validation`() {
        assertEquals(MissionValidationError.EMPTY_TITLE, SaveMissionUseCase.validate("  ", 4))
        assertEquals(MissionValidationError.INVALID_ESTIMATE, SaveMissionUseCase.validate("Read", 0))
        assertEquals(null, SaveMissionUseCase.validate("Read", 4))
    }
}
