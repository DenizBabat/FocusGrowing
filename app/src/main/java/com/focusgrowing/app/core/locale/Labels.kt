package com.focusgrowing.app.core.locale

import androidx.annotation.StringRes
import com.focusgrowing.app.R
import com.focusgrowing.app.domain.model.BackgroundScene
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import com.focusgrowing.app.domain.model.WorldItemType

/*
 * String resource for each domain enum value. Kept in one place so that both Composables
 * (stringResource(x.nameRes())) and non-UI code (StringProvider.get(x.nameRes())) translate the same way.
 */

@StringRes
fun MissionPriority.nameRes(): Int = when (this) {
    MissionPriority.LOW -> R.string.priority_low
    MissionPriority.MEDIUM -> R.string.priority_medium
    MissionPriority.HIGH -> R.string.priority_high
}

@StringRes
fun MissionCategory.nameRes(): Int = when (this) {
    MissionCategory.GENERAL -> R.string.category_general
    MissionCategory.DEVELOPMENT -> R.string.category_development
    MissionCategory.DESIGN -> R.string.category_design
    MissionCategory.STUDY -> R.string.category_study
    MissionCategory.READING -> R.string.category_reading
    MissionCategory.WORK -> R.string.category_work
    MissionCategory.HEALTH -> R.string.category_health
    MissionCategory.PERSONAL -> R.string.category_personal
}

@StringRes
fun WorldItemType.nameRes(): Int = when (this) {
    WorldItemType.SPROUT -> R.string.world_item_sprout
    WorldItemType.GRASS -> R.string.world_item_grass
    WorldItemType.FLOWERS -> R.string.world_item_flowers
    WorldItemType.BUSH -> R.string.world_item_bush
    WorldItemType.PINE -> R.string.world_item_pine
    WorldItemType.ROCKS -> R.string.world_item_rocks
    WorldItemType.HOUSE -> R.string.world_item_house
    WorldItemType.FENCE -> R.string.world_item_fence
    WorldItemType.GARDEN -> R.string.world_item_garden
    WorldItemType.LAKE -> R.string.world_item_lake
    WorldItemType.OAK -> R.string.world_item_oak
    WorldItemType.WATERFALL -> R.string.world_item_waterfall
    WorldItemType.CABIN -> R.string.world_item_cabin
    WorldItemType.WINDMILL -> R.string.world_item_windmill
    WorldItemType.BRIDGE -> R.string.world_item_bridge
    WorldItemType.LIGHTHOUSE -> R.string.world_item_lighthouse
}

@StringRes
fun BackgroundScene.nameRes(): Int = when (this) {
    BackgroundScene.MOUNTAIN_LAKE -> R.string.scene_mountain_lake
    BackgroundScene.FOREST_MORNING -> R.string.scene_forest_morning
    BackgroundScene.SUNSET_HILLS -> R.string.scene_sunset_hills
    BackgroundScene.CITY_DUSK -> R.string.scene_city_dusk
    BackgroundScene.CITY_NIGHT -> R.string.scene_city_night
    BackgroundScene.ABSTRACT_BLOBS -> R.string.scene_abstract_blobs
    BackgroundScene.AURORA -> R.string.scene_aurora
    BackgroundScene.NIGHT_SKY -> R.string.scene_night_sky
    BackgroundScene.MINIMAL_WAVES -> R.string.scene_minimal_waves
    BackgroundScene.DEEP_DARK -> R.string.scene_deep_dark
}
