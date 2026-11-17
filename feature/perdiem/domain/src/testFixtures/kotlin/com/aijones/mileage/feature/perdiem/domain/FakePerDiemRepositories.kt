package com.aijones.mileage.feature.perdiem.domain

import com.aijones.mileage.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.Month

object SamplePerDiem {
    private fun dollars(d: String) = Money(java.math.BigDecimal(d).movePointRight(6).longValueExact())

    private fun mie(total: String, b: String, l: String, d: String, firstLast: String) =
        MieBreakdown(dollars(total), dollars(b), dollars(l), dollars(d), dollars("5"), dollars(firstLast))

    private fun months(vararg pairs: Pair<Month, String>, otherwise: String) =
        Month.entries.associateWith { m -> dollars(pairs.toMap()[m] ?: otherwise) }

    val STANDARD_LOCATION = PerDiemLocation(1, "Standard rate", "CONUS", null, 2027, isStandard = true)
    val NYC_LOCATION = PerDiemLocation(2, "New York City", "NY", "New York", 2027, isStandard = false)
    val BOSTON_LOCATION = PerDiemLocation(3, "Boston", "MA", "Suffolk", 2027, isStandard = false)

    val STANDARD = PerDiemRates(STANDARD_LOCATION, months(otherwise = "113"), mie("68", "16", "19", "28", "51"))
    val NYC = PerDiemRates(
        NYC_LOCATION,
        months(Month.OCTOBER to "330", Month.NOVEMBER to "286", Month.DECEMBER to "286", Month.JANUARY to "175", Month.FEBRUARY to "175", otherwise = "260"),
        mie("92", "23", "26", "38", "69"),
    )
    val BOSTON = PerDiemRates(BOSTON_LOCATION, months(Month.NOVEMBER to "239", otherwise = "250"), mie("86", "22", "25", "34", "64.50"))

    val all = listOf(STANDARD, NYC, BOSTON)
}

class FakePerDiemRateRepository(private val data: List<PerDiemRates> = SamplePerDiem.all) : PerDiemRateRepository {
    override suspend fun search(query: String, fiscalYear: Int): List<PerDiemLocation> = data.map { it.location }.filter {
        !it.isStandard && it.fiscalYear == fiscalYear &&
            (it.city.contains(query, ignoreCase = true) || it.county?.contains(query, ignoreCase = true) == true || it.state.equals(query, ignoreCase = true))
    }

    override suspend fun rates(locationId: Long): PerDiemRates? = data.firstOrNull { it.location.id == locationId }

    override suspend fun standardRates(fiscalYear: Int): PerDiemRates = data.first { it.location.isStandard }
}

class FakeStayRepository(initial: List<Stay> = emptyList()) : StayRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val stays: List<Stay> get() = state.value

    override fun observeOverlapping(from: LocalDate, to: LocalDate): Flow<List<Stay>> =
        state.map { list -> list.filter { !it.checkOut.isBefore(from) && !it.checkIn.isAfter(to) }.sortedBy { it.checkIn } }

    override suspend fun add(stay: Stay): Long {
        val id = nextId++
        state.update { it + stay.copy(id = id) }
        return id
    }

    override suspend fun delete(id: Long) = state.update { list -> list.filterNot { it.id == id } }
}

class FakeRecentSearchRepository(private val limit: Int = 5) : RecentSearchRepository {
    private val state = MutableStateFlow<List<PerDiemLocation>>(emptyList())

    override fun observe(): Flow<List<PerDiemLocation>> = state

    override suspend fun record(location: PerDiemLocation) = state.update { (listOf(location) + it.filterNot { r -> r.id == location.id }).take(limit) }
}
