package com.focusgrowing.app.domain.model

enum class InsightKind { TIME_OF_DAY, SESSION_LENGTH, ESTIMATION, CONSISTENCY, INTERRUPTIONS, BEST_DAY, GETTING_STARTED }

data class Insight(
    val id: String,
    val kind: InsightKind,
    val title: String,
    val message: String,
    /** Advanced insights are shown locked to free users. */
    val isPremium: Boolean,
)
