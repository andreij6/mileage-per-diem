package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class GetMileageRateUseCase @Inject constructor(private val rates: MileageRateRepository) {
    suspend operator fun invoke(date: LocalDate): Money =
        rates.observeCustomRate().first() ?: irsRateOn(rates.irsRates(), date)
}

class ObserveCurrentRateUseCase @Inject constructor(private val rates: MileageRateRepository, private val clock: Clock) {
    operator fun invoke(): Flow<Money> = rates.observeCustomRate().map { custom ->
        custom ?: irsRateOn(rates.irsRates(), LocalDate.now(clock))
    }
}

fun irsRateOn(table: List<MileageRate>, date: LocalDate): Money {
    val sorted = table.sortedBy { it.from }
    require(sorted.isNotEmpty()) { "IRS rate table is empty" }
    return (sorted.firstOrNull { date in it } ?: sorted.lastOrNull { !it.from.isAfter(date) } ?: sorted.first()).perMile
}
