package com.aijones.mileage.core.analytics

class FakeAnalytics : Analytics {
    private val recorded = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = recorded.toList()

    override fun track(event: AnalyticsEvent) {
        recorded += event
    }

    inline fun <reified T : AnalyticsEvent> eventsOf(): List<T> = events.filterIsInstance<T>()

    fun clear() = recorded.clear()
}
