package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

object SampleRates {
    val IRS_2026_H1 = MileageRate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), Money(725_000), "IRS Notice 2025-5")
    val IRS_2026_H2 = MileageRate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), Money(760_000), "IRB 2026-29")
    val irs2026 = listOf(IRS_2026_H1, IRS_2026_H2)
}

class FakeMileageRateRepository(
    private val rates: List<MileageRate> = SampleRates.irs2026,
    customRate: Money? = null,
) : MileageRateRepository {
    private val custom = MutableStateFlow(customRate)

    val customRate: Money? get() = custom.value

    override suspend fun irsRates(): List<MileageRate> = rates

    override fun observeCustomRate(): Flow<Money?> = custom

    override suspend fun setCustomRate(rate: Money?) {
        custom.value = rate
    }
}
