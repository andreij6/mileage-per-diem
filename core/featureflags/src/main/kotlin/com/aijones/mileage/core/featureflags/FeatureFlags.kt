package com.aijones.mileage.core.featureflags

import kotlinx.coroutines.flow.Flow

enum class FeatureFlag(val key: String, val default: Boolean, val title: String, val description: String) {
    YEAR_IN_REVIEW("year_in_review", true, "Year in review", "Stories on the Insights screen"),
    DRIVE_DETECTION("drive_detection", true, "Drive detection", "Suggest trips while driving"),
    ROUTE_REPLAY("route_replay", true, "Route replay", "Scrubber and speed graph on trip detail"),
    LIVE_UPDATE("live_update", false, "Live Update notification", "Android 16 promoted notification"),
}

enum class FlagSource { DEFAULT, OVERRIDE }

data class FlagState(val flag: FeatureFlag, val enabled: Boolean, val source: FlagSource)

interface FeatureFlags {
    fun observe(flag: FeatureFlag): Flow<Boolean>
    fun observeAll(): Flow<List<FlagState>>
    suspend fun isEnabled(flag: FeatureFlag): Boolean
    suspend fun setOverride(flag: FeatureFlag, enabled: Boolean?)
    suspend fun resetOverrides()
}
