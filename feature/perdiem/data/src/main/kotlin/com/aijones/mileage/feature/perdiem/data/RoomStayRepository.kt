package com.aijones.mileage.feature.perdiem.data

import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.database.dao.StayDao
import com.aijones.mileage.core.database.entity.StayEntity
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.perdiem.domain.StayRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class RoomStayRepository @Inject constructor(
    private val dao: StayDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StayRepository {
    override fun observeOverlapping(from: LocalDate, to: LocalDate): Flow<List<Stay>> =
        dao.observeOverlapping(from.toEpochDay(), to.toEpochDay()).map { list -> list.map { it.toDomain() } }.flowOn(io)

    override suspend fun add(stay: Stay): Long = withContext(io) { dao.insert(stay.toEntity()) }

    override suspend fun delete(id: Long) = withContext(io) { dao.delete(id) }
}

internal fun StayEntity.toDomain() = Stay(
    id = id,
    locationId = locationId,
    locationLabel = locationLabel,
    fiscalYear = fiscalYear,
    checkIn = LocalDate.ofEpochDay(checkInEpochDay),
    checkOut = LocalDate.ofEpochDay(checkOutEpochDay),
    includeLodging = includeLodging,
    mieDaily = Money.ofCents(mieDailyCents),
    mieTotal = Money.ofCents(mieTotalCents),
    lodgingTotal = Money.ofCents(lodgingTotalCents),
)

internal fun Stay.toEntity() = StayEntity(
    id = id,
    locationId = locationId,
    locationLabel = locationLabel,
    fiscalYear = fiscalYear,
    checkInEpochDay = checkIn.toEpochDay(),
    checkOutEpochDay = checkOut.toEpochDay(),
    includeLodging = includeLodging,
    mieDailyCents = mieDaily.cents,
    mieTotalCents = mieTotal.cents,
    lodgingTotalCents = lodgingTotal.cents,
)
