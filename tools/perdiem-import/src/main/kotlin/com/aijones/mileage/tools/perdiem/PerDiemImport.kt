package com.aijones.mileage.tools.perdiem

import com.aijones.mileage.tools.perdiem.gsa.models.LodgingRate
import com.aijones.mileage.tools.perdiem.gsa.models.MieRate
import java.math.BigDecimal
import java.math.RoundingMode

data class ImportedLocation(
    val id: Long,
    val city: String,
    val state: String,
    val county: String?,
    val isStandard: Boolean,
    val mieTotalCents: Long,
    val lodgingCentsByMonth: Map<Int, Long>,
)

data class ImportedMie(
    val totalCents: Long,
    val breakfastCents: Long,
    val lunchCents: Long,
    val dinnerCents: Long,
    val incidentalsCents: Long,
    val firstLastCents: Long,
)

data class ImportedYear(val fiscalYear: Int, val locations: List<ImportedLocation>, val mie: List<ImportedMie>)

const val STANDARD_CITY = "Standard rate"
const val STANDARD_STATE = "CONUS"
private const val GSA_STANDARD_CITY = "Standard Rate"

fun mapGsa(fiscalYear: Int, lodging: List<LodgingRate>, mie: List<MieRate>): ImportedYear {
    val tiers = mie.map {
        ImportedMie(cents(it.total), cents(it.breakfast), cents(it.lunch), cents(it.dinner), cents(it.incidental), cents(it.firstLastDay))
    }.sortedBy { it.totalCents }
    val (standardRows, destinations) = lodging.partition { it.city.equals(GSA_STANDARD_CITY, ignoreCase = true) }
    require(standardRows.isNotEmpty()) { "GSA response has no standard rate rows" }
    val standardRates = standardRows.map { it.months() to cents(it.meals) }.distinct()
    require(standardRates.size == 1) { "standard rate rows disagree: $standardRates" }
    val standard = ImportedLocation(1, STANDARD_CITY, STANDARD_STATE, null, true, standardRates.single().second, standardRates.single().first)
    val others = destinations
        .sortedWith(compareBy({ it.state }, { it.city }, { it.county.orEmpty() }))
        .mapIndexed { i, row ->
            ImportedLocation(i + 2L, row.city.trim(), row.state.trim(), row.county?.trim()?.ifEmpty { null }, false, cents(row.meals), row.months())
        }
    val locations = listOf(standard) + others
    val known = tiers.map { it.totalCents }.toSet()
    locations.firstOrNull { it.mieTotalCents !in known }?.let { error("${it.city}, ${it.state}: no M&IE tier for ${it.mieTotalCents} cents") }
    return ImportedYear(fiscalYear, locations, tiers)
}

private fun LodgingRate.months(): Map<Int, Long> =
    listOf(jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec).mapIndexed { i, v -> i + 1 to cents(v) }.toMap()

private fun cents(dollars: String): Long = BigDecimal(dollars.trim()).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()

private fun cents(dollars: Double): Long = BigDecimal.valueOf(dollars).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
