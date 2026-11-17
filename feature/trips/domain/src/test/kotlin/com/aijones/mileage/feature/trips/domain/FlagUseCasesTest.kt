package com.aijones.mileage.feature.trips.domain

import app.cash.turbine.test
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlagUseCasesTest {
    @Test
    fun routeReplayFollowsTheFlag() = runTest {
        val flags = FakeFeatureFlags()
        ObserveRouteReplayEnabledUseCase(flags)().test {
            assertTrue(awaitItem())
            flags.setOverride(FeatureFlag.ROUTE_REPLAY, false)
            assertFalse(awaitItem())
        }
    }
}
