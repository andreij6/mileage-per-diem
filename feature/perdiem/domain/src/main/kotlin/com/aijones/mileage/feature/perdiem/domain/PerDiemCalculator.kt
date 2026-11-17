package com.aijones.mileage.feature.perdiem.domain

import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.sum
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StayLine(val date: LocalDate, val mie: Money, val lodging: Money, val isTravelDay: Boolean)

data class StayEstimate(
    val days: Int,
    val travelDays: Int,
    val fullDays: Int,
    val mieTotal: Money,
    val nights: Int,
    val lodgingTotal: Money,
    val total: Money,
    val lines: List<StayLine>,
)

object PerDiemCalculator {
    fun estimate(rates: PerDiemRates, checkIn: LocalDate, checkOut: LocalDate, includeLodging: Boolean): StayEstimate {
        require(!checkOut.isBefore(checkIn)) { "check-out must not be before check-in" }
        val days = ChronoUnit.DAYS.between(checkIn, checkOut).toInt() + 1
        val lines = (0 until days).map { i ->
            val date = checkIn.plusDays(i.toLong())
            val travelDay = date == checkIn || date == checkOut
            val lodging = if (includeLodging && date != checkOut) rates.lodgingFor(date) else Money.ZERO
            StayLine(date, if (travelDay) rates.mie.firstLastDay else rates.mie.total, lodging, travelDay)
        }
        val mieTotal = lines.map { it.mie }.sum()
        val lodgingTotal = lines.map { it.lodging }.sum()
        val travelDays = lines.count { it.isTravelDay }
        return StayEstimate(
            days = days,
            travelDays = travelDays,
            fullDays = days - travelDays,
            mieTotal = mieTotal,
            nights = days - 1,
            lodgingTotal = lodgingTotal,
            total = mieTotal + lodgingTotal,
            lines = lines,
        )
    }
}
