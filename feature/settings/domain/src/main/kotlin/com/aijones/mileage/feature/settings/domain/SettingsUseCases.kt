package com.aijones.mileage.feature.settings.domain

import com.aijones.mileage.core.analytics.EventLog
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FeatureFlags
import com.aijones.mileage.core.featureflags.FlagState
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.MileageRateRepository
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Settings> = settings.observe()
}

class UpdateSettingsUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(transform: (Settings) -> Settings) = settings.update(transform)
}

class ObserveFeatureFlagsUseCase @Inject constructor(private val flags: FeatureFlags) {
    operator fun invoke(): Flow<List<FlagState>> = flags.observeAll()
}

class SetFeatureFlagOverrideUseCase @Inject constructor(private val flags: FeatureFlags) {
    suspend operator fun invoke(flag: FeatureFlag, enabled: Boolean?) = flags.setOverride(flag, enabled)
}

class ResetFeatureFlagOverridesUseCase @Inject constructor(private val flags: FeatureFlags) {
    suspend operator fun invoke() = flags.resetOverrides()
}

class ObserveAppHealthUseCase @Inject constructor(private val eventLog: EventLog) {
    operator fun invoke(since: Instant): Flow<AppHealth> =
        combine(eventLog.observeSummary(since.toEpochMilli()), eventLog.observeRecent(RECENT_EVENTS), ::AppHealth)

    companion object {
        const val RECENT_EVENTS = 20
    }
}

class ExportEventLogUseCase @Inject constructor(private val eventLog: EventLog) {
    suspend operator fun invoke(): String = eventLog.exportNdjson()
}

class ObserveMileageRateSettingsUseCase @Inject constructor(
    private val rates: MileageRateRepository,
    private val observeCurrentRate: ObserveCurrentRateUseCase,
) {
    operator fun invoke(): Flow<MileageRateSettings> =
        combine(flow { emit(rates.irsRates()) }, rates.observeCustomRate(), observeCurrentRate(), ::MileageRateSettings)
}

class SetCustomRateUseCase @Inject constructor(private val rates: MileageRateRepository) {
    suspend operator fun invoke(rate: Money?) {
        require(rate == null || rate > Money.ZERO) { "custom rate must be positive" }
        rates.setCustomRate(rate)
    }
}

class DeleteAllDataUseCase @Inject constructor(private val resetter: DataResetter) {
    suspend operator fun invoke() = resetter.deleteAllTripsAndReports()
}

class UnlockDeveloperOptionsUseCase @Inject constructor(private val settings: SettingsRepository) {
    private var taps = 0

    suspend operator fun invoke(): Int {
        if (settings.observe().first().developerUnlocked) return 0
        taps++
        val remaining = (TAPS_TO_UNLOCK - taps).coerceAtLeast(0)
        if (remaining == 0) settings.update { it.copy(developerUnlocked = true) }
        return remaining
    }

    companion object {
        const val TAPS_TO_UNLOCK = 7
    }
}
