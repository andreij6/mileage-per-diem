package com.aijones.mileage.core.analytics

sealed class AnalyticsEvent(val name: String) {
    abstract val params: Map<String, Any>

    data class AppStartup(val type: StartType, val ttidMs: Long, val ttfdMs: Long?) : AnalyticsEvent("app_startup") {
        override val params get() = buildMap { put("type", type.wire); put("ttid_ms", ttidMs); ttfdMs?.let { put("ttfd_ms", it) } }
    }

    data object SessionStart : AnalyticsEvent("session_start") {
        override val params get() = emptyMap<String, Any>()
    }

    data class SessionEnd(val durationS: Long) : AnalyticsEvent("session_end") {
        override val params get() = mapOf("duration_s" to durationS)
    }

    data class ScreenView(val screen: String) : AnalyticsEvent("screen_view") {
        override val params get() = mapOf("screen" to screen)
    }

    data class OnboardingStep(val step: String, val result: String) : AnalyticsEvent("onboarding_step") {
        override val params get() = mapOf("step" to step, "result" to result)
    }

    data class PermissionResult(val permission: String, val granted: Boolean, val precise: Boolean? = null) :
        AnalyticsEvent("permission_result") {
        override val params get() = buildMap { put("permission", permission); put("granted", granted); precise?.let { put("precise", it) } }
    }

    data class TripRecordingStarted(val source: TripSource) : AnalyticsEvent("trip_recording_started") {
        override val params get() = mapOf("source" to source.wire)
    }

    data class TripRecordingStopped(
        val distanceTenthsMi: Long,
        val durationS: Long,
        val fixes: Int,
        val droppedFixes: Int,
        val interrupted: Boolean,
    ) : AnalyticsEvent("trip_recording_stopped") {
        override val params get() = mapOf(
            "distance_mi" to distanceTenthsMi / 10.0, "duration_s" to durationS, "fixes" to fixes,
            "dropped_fixes" to droppedFixes, "interrupted" to interrupted,
        )
    }

    data class TripRecordingFailed(val reason: FailureReason) : AnalyticsEvent("trip_recording_failed") {
        override val params get() = mapOf("reason" to reason.wire)
    }

    data class LocationPolicyChanged(val policy: String) : AnalyticsEvent("location_policy_changed") {
        override val params get() = mapOf("policy" to policy)
    }

    data class TripClassified(val category: String, val via: String) : AnalyticsEvent("trip_classified") {
        override val params get() = mapOf("category" to category, "via" to via)
    }

    data class TripEdited(val fields: List<String>) : AnalyticsEvent("trip_edited") {
        override val params get() = mapOf("fields" to fields.joinToString(","))
    }

    data class TripAddedManual(val distanceTenthsMi: Long) : AnalyticsEvent("trip_added_manual") {
        override val params get() = mapOf("distance_mi" to distanceTenthsMi / 10.0)
    }

    data object TripDeleted : AnalyticsEvent("trip_deleted") {
        override val params get() = emptyMap<String, Any>()
    }

    data class PerDiemSearched(val resultCount: Int) : AnalyticsEvent("per_diem_searched") {
        override val params get() = mapOf("result_count" to resultCount)
    }

    data class StayAdded(val days: Int, val lodging: Boolean) : AnalyticsEvent("stay_added") {
        override val params get() = mapOf("days" to days, "lodging" to lodging)
    }

    data class ExportCompleted(val format: String, val trips: Int, val durationMs: Long) : AnalyticsEvent("export_completed") {
        override val params get() = mapOf("format" to format, "trips" to trips, "duration_ms" to durationMs)
    }

    data class ExportFailed(val format: String, val reason: String) : AnalyticsEvent("export_failed") {
        override val params get() = mapOf("format" to format, "reason" to reason)
    }

    data object InsightsViewed : AnalyticsEvent("insights_viewed") {
        override val params get() = emptyMap<String, Any>()
    }

    data class YearInReviewViewed(val story: Int) : AnalyticsEvent("year_in_review_viewed") {
        override val params get() = mapOf("story" to story)
    }

    data class ShareCardExported(val story: Int) : AnalyticsEvent("share_card_exported") {
        override val params get() = mapOf("story" to story)
    }

    data class WidgetAction(val widget: String, val action: String) : AnalyticsEvent("widget_action") {
        override val params get() = mapOf("widget" to widget, "action" to action)
    }

    data class FeatureFlagExposed(val flag: String, val value: Boolean, val source: String) : AnalyticsEvent("feature_flag_exposed") {
        override val params get() = mapOf("flag" to flag, "value" to value, "source" to source)
    }

    data class AppCrash(val exception: String, val thread: String) : AnalyticsEvent("app_crash") {
        override val params get() = mapOf("exception" to exception, "thread" to thread)
    }
}

enum class StartType(val wire: String) { COLD("cold"), WARM("warm") }

enum class TripSource(val wire: String) { APP("app"), NOTIFICATION("notification"), WIDGET("widget"), TILE("tile") }

enum class FailureReason(val wire: String) {
    FGS_START_FAILED("fgs_start_failed"),
    PERMISSION_REVOKED("permission_revoked"),
    LOCATION_OFF("location_off"),
}
