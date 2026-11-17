package com.aijones.mileage.feature.widgets.presentation

import android.service.quicksettings.Tile
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.SamplePerDiem
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.trips.domain.MonthSummary
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.test.assertEquals

class WidgetMappingTest {
    private val rate = Money(760_000)
    private val recording = RecordingState.Recording(
        tripId = 7,
        startedAt = Instant.parse("2026-11-16T14:00:00Z"),
        distance = Distance.ofMiles(12.6),
        elapsed = Duration.ofMinutes(24).plusSeconds(18),
        accuracyM = 5f,
        speedMps = 12f,
        policy = RecordingPolicy.HIGH,
    )

    @Test
    fun monthSummaryBecomesAmountAndOdometerDigits() {
        val summary = MonthSummary(YearMonth.of(2026, 11), Money.ofCents(64_220), Distance.ofMiles(845.0), 31, rate)

        assertEquals(MonthWidgetUi("November", "$642.20", "0845", "0"), summary.toWidgetUi(Locale.US))
    }

    @Test
    fun perDiemShowsWholeDollarMealsAndThatNightsLodging() {
        assertEquals(PerDiemWidgetUi("$92", "New York City", "$286"), SamplePerDiem.NYC.toWidgetUi(LocalDate.of(2026, 11, 16)))
        assertEquals("$330", SamplePerDiem.NYC.toWidgetUi(LocalDate.of(2026, 10, 2)).lodging)
        assertEquals("$64.50", Money(64_500_000).formatUsdShort())
    }

    @Test
    fun recordingStatesMapToLiveTextsAndAmountSoFar() {
        assertEquals(RecordingWidgetUi.Active(false, "12.6 mi", "24:18", "$9.58"), recording.toWidgetUi(rate))
        assertEquals(
            RecordingWidgetUi.Active(true, "12.6 mi", "1:02:03", "$9.58"),
            RecordingState.Paused(7, recording.startedAt, recording.distance, Duration.ofSeconds(3_723)).toWidgetUi(rate),
        )
        assertEquals(RecordingWidgetUi.Idle, RecordingState.Idle.toWidgetUi(rate))
    }

    @Test
    fun tileIsActiveWhileATripIsRecordingOrPaused() {
        assertEquals(Tile.STATE_INACTIVE, RecordingState.Idle.tileState())
        assertEquals(Tile.STATE_ACTIVE, recording.tileState())
        assertEquals(Tile.STATE_ACTIVE, RecordingState.Paused(7, recording.startedAt, recording.distance, recording.elapsed).tileState())
    }
}
