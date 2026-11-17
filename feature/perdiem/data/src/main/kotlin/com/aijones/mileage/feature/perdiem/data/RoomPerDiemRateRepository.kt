package com.aijones.mileage.feature.perdiem.data

import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.MieBreakdown
import com.aijones.mileage.feature.perdiem.domain.PerDiemLocation
import com.aijones.mileage.feature.perdiem.domain.PerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.PerDiemRates
import dagger.Lazy
import java.time.Month
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class RoomPerDiemRateRepository @Inject constructor(
    private val database: Lazy<PerDiemDatabase>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PerDiemRateRepository {
    private val dao: PerDiemDao get() = database.get().dao()

    override suspend fun search(query: String, fiscalYear: Int): List<PerDiemLocation> = withContext(io) {
        val match = ftsPrefixMatch(query) ?: return@withContext emptyList()
        dao.search(match, fiscalYear, SEARCH_LIMIT).map { it.toDomain() }
    }

    override suspend fun rates(locationId: Long): PerDiemRates? = withContext(io) {
        val location = dao.location(locationId) ?: dao.latestStandard() ?: return@withContext null
        ratesFor(location)
    }

    override suspend fun standardRates(fiscalYear: Int): PerDiemRates = withContext(io) {
        ratesFor(checkNotNull(dao.standard(fiscalYear) ?: dao.latestStandard()) { "perdiem.db has no standard rate" })
    }

    private suspend fun ratesFor(location: LocationEntity): PerDiemRates {
        val mie = checkNotNull(dao.mie(location.fy, location.mieTotalCents)) { "no M&IE tier ${location.mieTotalCents} for FY${location.fy}" }
        return PerDiemRates(
            location = location.toDomain(),
            lodgingByMonth = dao.lodging(location.id).associate { Month.of(it.month) to Money.ofCents(it.cents) },
            mie = MieBreakdown(
                total = Money.ofCents(mie.totalCents),
                breakfast = Money.ofCents(mie.breakfastCents),
                lunch = Money.ofCents(mie.lunchCents),
                dinner = Money.ofCents(mie.dinnerCents),
                incidentals = Money.ofCents(mie.incidentalsCents),
                firstLastDay = Money.ofCents(mie.firstLastCents),
            ),
        )
    }

    private companion object {
        const val SEARCH_LIMIT = 50
    }
}

internal fun ftsPrefixMatch(query: String): String? =
    query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.joinToString(" ") { "$it*" }

internal fun LocationEntity.toDomain() = PerDiemLocation(id, city, state, county, fy, isStandard)
