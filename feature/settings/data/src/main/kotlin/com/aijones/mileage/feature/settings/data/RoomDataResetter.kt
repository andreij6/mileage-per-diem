package com.aijones.mileage.feature.settings.data

import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.database.dao.StayDao
import com.aijones.mileage.core.database.dao.TripDao
import com.aijones.mileage.feature.settings.domain.DataResetter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class RoomDataResetter @Inject constructor(
    private val trips: TripDao,
    private val stays: StayDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DataResetter {
    override suspend fun deleteAllTripsAndReports() = withContext(io) {
        trips.deleteAll()
        stays.deleteAll()
    }
}
