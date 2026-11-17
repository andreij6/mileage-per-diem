package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FeatureFlags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRouteReplayEnabledUseCase @Inject constructor(private val flags: FeatureFlags) {
    operator fun invoke(): Flow<Boolean> = flags.observe(FeatureFlag.ROUTE_REPLAY)
}
