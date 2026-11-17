package com.aijones.mileage.feature.widgets.presentation

import android.service.quicksettings.Tile
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.groupThousands
import com.aijones.mileage.core.model.mileageAmount
import com.aijones.mileage.feature.perdiem.domain.PerDiemRates
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.trips.domain.MonthSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import java.time.Duration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class MonthWidgetUi(val monthName: String, val amount: String, val whole: String, val tenth: String)

data class PerDiemWidgetUi(val mie: String, val city: String, val lodging: String)

sealed interface RecordingWidgetUi {
    data object Idle : RecordingWidgetUi
    data class Active(val paused: Boolean, val distance: String, val elapsed: String, val soFar: String) : RecordingWidgetUi
}

data class WidgetSnapshot(val month: MonthWidgetUi, val perDiem: PerDiemWidgetUi?, val recording: RecordingWidgetUi)

fun MonthSummary.toWidgetUi(locale: Locale) = MonthWidgetUi(
    monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale),
    amount = reimbursement.formatUsd(),
    whole = (distance.tenthsOfMile / 10).toString().padStart(4, '0'),
    tenth = (distance.tenthsOfMile % 10).toString(),
)

fun PerDiemRates.toWidgetUi(today: LocalDate) = PerDiemWidgetUi(
    mie = mie.total.formatUsdShort(),
    city = location.city,
    lodging = lodgingFor(today).formatUsdShort(),
)

fun RecordingState.toWidgetUi(rate: Money): RecordingWidgetUi = when (this) {
    RecordingState.Idle -> RecordingWidgetUi.Idle
    is RecordingState.Recording -> RecordingWidgetUi.Active(false, distance.formatMiles(), elapsed.formatElapsed(), mileageAmount(distance, rate).formatUsd())
    is RecordingState.Paused -> RecordingWidgetUi.Active(true, distance.formatMiles(), elapsed.formatElapsed(), mileageAmount(distance, rate).formatUsd())
}

fun RecordingState.tileState(): Int = if (this == RecordingState.Idle) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE

fun Duration.formatElapsed(): String {
    val s = seconds.coerceAtLeast(0)
    val mmss = "%02d:%02d".format(Locale.US, s / 60 % 60, s % 60)
    return if (s >= 3_600) "${s / 3_600}:$mmss" else mmss
}

fun Money.formatUsdShort(): String = if (cents % 100 == 0L) "$" + groupThousands(cents / 100) else formatUsd()

fun Flow<WidgetSnapshot>.throttleWhileRecording(windowMs: Long, now: () -> Long): Flow<WidgetSnapshot> {
    var lastAt = 0L
    var lastActive = false
    return distinctUntilChanged().transformLatest { snapshot ->
        val active = snapshot.recording is RecordingWidgetUi.Active
        if (active && lastActive) {
            val wait = lastAt + windowMs - now()
            if (wait > 0) delay(wait)
        }
        lastAt = now()
        lastActive = active
        emit(snapshot)
    }
}
