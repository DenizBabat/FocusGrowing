package com.focusgrowing.app.domain.model

data class WorldState(
    val totalXp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val completedSessions: Int,
) {
    val progress: Float
        get() = if (xpForNextLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)

    companion object {
        val Empty = WorldState(totalXp = 0, level = 1, xpIntoLevel = 0, xpForNextLevel = 100, completedSessions = 0)
    }
}

/** Objects that appear on the world island. Positions/drawing live in the UI layer. */
enum class WorldItemType(val unlockLevel: Int, val displayName: String, val isPremium: Boolean = false) {
    SPROUT(1, "Sprout"),
    GRASS(1, "Grass"),
    FLOWERS(2, "Flowers"),
    BUSH(2, "Bushes"),
    PINE(3, "Pine trees"),
    ROCKS(3, "Rocks"),
    HOUSE(4, "House"),
    FENCE(4, "Fence"),
    GARDEN(4, "Garden"),
    LAKE(4, "Lake"),
    OAK(5, "Oak trees"),
    WATERFALL(5, "Waterfall"),
    CABIN(6, "Village cabin"),
    WINDMILL(6, "Windmill", isPremium = true),
    BRIDGE(7, "Bridge", isPremium = true),
    LIGHTHOUSE(8, "Lighthouse", isPremium = true),
}
