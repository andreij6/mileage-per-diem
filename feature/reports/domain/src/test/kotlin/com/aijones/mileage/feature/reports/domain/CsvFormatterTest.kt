package com.aijones.mileage.feature.reports.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class CsvFormatterTest {
    private val zone = ZoneId.of("America/New_York")

    private val trip = Trip(
        id = 1, startedAt = Instant.parse("2026-11-03T02:00:00Z"), status = TripStatus.DONE, distance = Distance.ofMiles(18.4),
        ratePerMile = Money(760_000), purpose = "Client visit", startLabel = "Office", endLabel = "Acme HQ",
    )
    private val stay = Stay(
        id = 1, locationId = 2, locationLabel = "New York City, NY", fiscalYear = 2027,
        checkIn = LocalDate.of(2026, 11, 18), checkOut = LocalDate.of(2026, 11, 20), includeLodging = true,
        mieDaily = Money.ofDollars(92), mieTotal = Money.ofDollars(230), lodgingTotal = Money.ofDollars(572),
    )

    private fun report(trips: List<Trip>, stays: List<Stay> = emptyList()): Report {
        val mileage = Money(trips.sumOf { it.reimbursement.micros })
        val perDiem = Money(stays.sumOf { it.total.micros })
        return Report(
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "Dana", "Acme", trips, stays, mileage, perDiem, mileage + perDiem,
            trips.fold(Distance.ZERO) { a, t -> a + t.distance }, Instant.parse("2026-11-16T14:41:00Z"), "IRS 76¢/mi",
        )
    }

    @Test
    fun formatsHeaderRowsAndTotalsWithCrlf() {
        val csv = CsvFormatter.format(report(listOf(trip), listOf(stay)), zone)
        val expected = listOf(
            "Type,Date,End date,Description,From,To,Miles,Rate per mile,Amount",
            "Mileage,2026-11-02,,Client visit,Office,Acme HQ,18.4,0.760,13.98",
            "Per diem,2026-11-18,2026-11-20,\"New York City, NY\",,,,,802.00",
            "Total,,,Mileage,,,18.4,,13.98",
            "Total,,,Per diem,,,,,802.00",
            "Total,,,All,,,,,815.98",
        ).joinToString("\r\n", postfix = "\r\n")
        assertEquals(expected, csv)
    }

    @Test
    fun everyRowHasTheSameFieldCount() {
        val csv = CsvFormatter.format(report(listOf(trip, trip.copy(purpose = null, ratePerMile = null)), listOf(stay.copy(locationLabel = "Boston"))), zone)
        csv.trimEnd().split("\r\n").forEach { assertEquals(9, it.split(",").size, it) }
    }

    @Test
    fun escapingTable() {
        listOf(
            "plain" to "plain",
            "" to "",
            "a,b" to "\"a,b\"",
            "say \"hi\"" to "\"say \"\"hi\"\"\"",
            "line1\nline2" to "\"line1\nline2\"",
            "cr\rlf" to "\"cr\rlf\"",
            "\"" to "\"\"\"\"",
        ).forEach { (input, expected) -> assertEquals(expected, CsvFormatter.escape(input), input) }
    }

    @Test
    fun freeTextIsEscapedInRows() {
        val tricky = trip.copy(purpose = "Lunch, \"Q4\" review\nwith team")
        val row = CsvFormatter.format(report(listOf(tricky)), zone).split("\r\n")[1]
        assertEquals("Mileage,2026-11-02,,\"Lunch, \"\"Q4\"\" review\nwith team\",Office,Acme HQ,18.4,0.760,13.98", row)
    }

    @Test
    fun emptyReportHasHeaderAndZeroTotals() {
        val lines = CsvFormatter.format(report(emptyList()), zone).trimEnd().split("\r\n")
        assertEquals(4, lines.size)
        assertEquals("Total,,,All,,,,,0.00", lines.last())
    }
}
