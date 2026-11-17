package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aijones.mileage.core.designsystem.component.MpdListRow
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel
    data class On(val text: String) : DayLabel
}

@Immutable
data class TripRowUi(
    val id: Long,
    val from: String?,
    val to: String?,
    val day: DayLabel,
    val times: String,
    val distance: String,
    val amount: String,
    val purpose: String?,
)

internal val clockTime: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.US)
internal val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
internal val weekdayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
internal val meridiemTime: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

internal fun Trip.localDate(clock: Clock): LocalDate = startedAt.atZone(clock.zone).toLocalDate()

internal fun dayLabel(date: LocalDate, clock: Clock, format: DateTimeFormatter = shortDate): DayLabel {
    val today = LocalDate.now(clock)
    return when (date) {
        today -> DayLabel.Today
        today.minusDays(1) -> DayLabel.Yesterday
        else -> DayLabel.On(date.format(format))
    }
}

internal fun Trip.timeRange(clock: Clock, format: DateTimeFormatter = clockTime): String {
    val start = startedAt.atZone(clock.zone).format(format)
    val end = endedAt?.atZone(clock.zone)?.format(format) ?: return start
    return "$start – $end"
}

internal fun Trip.toRowUi(clock: Clock) = TripRowUi(
    id = id,
    from = startLabel,
    to = endLabel,
    day = dayLabel(localDate(clock), clock),
    times = timeRange(clock),
    distance = distance.formatMiles(),
    amount = reimbursement.formatUsd(),
    purpose = purpose,
)

internal fun parseMiles(text: String): Distance? =
    text.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.takeIf { it > 0 }?.let(Distance::ofMiles)

@Composable
internal fun DayLabel.text(): String = when (this) {
    DayLabel.Today -> stringResource(R.string.trips_today)
    DayLabel.Yesterday -> stringResource(R.string.trips_yesterday)
    is DayLabel.On -> text
}

@Composable
internal fun routeTitle(from: String?, to: String?): String {
    val unknown = stringResource(R.string.trips_unknown_place)
    return stringResource(R.string.trips_route, from ?: unknown, to ?: unknown)
}

@Composable
internal fun TripCategory.label(): String =
    stringResource(if (this == TripCategory.BUSINESS) R.string.trips_business else R.string.trips_personal)

@Composable
internal fun TripRow(row: TripRowUi, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier, showPurpose: Boolean = true) {
    MpdListRow(
        title = routeTitle(row.from, row.to),
        modifier = modifier,
        subtitle = subtitle,
        icon = MpdIcons.Route,
        iconBg = MpdTheme.colors.primaryC,
        iconFg = MpdTheme.colors.onPrimaryC,
        amount = row.amount,
        amountCaption = if (showPurpose) row.purpose ?: stringResource(R.string.trips_no_purpose) else null,
        onClick = onClick,
    )
}
