package com.focusgrowing.app.domain.model

enum class BackgroundSource { DEFAULT, GALLERY, CAMERA, FILE }

enum class BackgroundCategory { MY_PHOTOS, NATURE, CITY, ABSTRACT, SPACE, MINIMAL, DARK }

/** Built-in backgrounds are drawn in code (no image files), which keeps the APK small. */
enum class BackgroundScene(val displayName: String, val category: BackgroundCategory) {
    MOUNTAIN_LAKE("Mountain Lake", BackgroundCategory.NATURE),
    FOREST_MORNING("Forest Morning", BackgroundCategory.NATURE),
    SUNSET_HILLS("Sunset Hills", BackgroundCategory.NATURE),
    CITY_DUSK("City Dusk", BackgroundCategory.CITY),
    CITY_NIGHT("City Night", BackgroundCategory.CITY),
    ABSTRACT_BLOBS("Soft Shapes", BackgroundCategory.ABSTRACT),
    AURORA("Aurora", BackgroundCategory.ABSTRACT),
    NIGHT_SKY("Night Sky", BackgroundCategory.SPACE),
    MINIMAL_WAVES("Calm Waves", BackgroundCategory.MINIMAL),
    DEEP_DARK("Deep Dark", BackgroundCategory.DARK),
}

data class BackgroundImage(
    /** "default:<SCENE>" for built-ins, "custom:<id>" for user images. */
    val id: String,
    val name: String,
    val source: BackgroundSource,
    val category: BackgroundCategory,
    val uri: String?,
    val scene: BackgroundScene?,
    val createdAt: Long,
    val isFavorite: Boolean,
    /** Crop focus point, -1 (left/top) .. 1 (right/bottom). */
    val alignX: Float = 0f,
    val alignY: Float = 0f,
    /** Extra zoom on top of the fill-crop, 1..3. */
    val zoom: Float = 1f,
) {
    val isDefault: Boolean get() = source == BackgroundSource.DEFAULT

    companion object {
        const val DEFAULT_PREFIX = "default:"
        const val CUSTOM_PREFIX = "custom:"
        val FallbackId = DEFAULT_PREFIX + BackgroundScene.MOUNTAIN_LAKE.name

        fun customId(dbId: Long) = "$CUSTOM_PREFIX$dbId"
        fun defaultId(scene: BackgroundScene) = "$DEFAULT_PREFIX${scene.name}"
    }
}
