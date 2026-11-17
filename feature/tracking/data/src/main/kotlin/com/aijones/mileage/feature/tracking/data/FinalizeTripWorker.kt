package com.aijones.mileage.feature.tracking.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aijones.mileage.feature.tracking.domain.FinalizeTripUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FinalizeTripWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val finalizeTrip: FinalizeTripUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val tripId = inputData.getLong(KEY_TRIP_ID, -1)
        if (tripId < 0) return Result.failure()
        finalizeTrip(tripId, inputData.getBoolean(KEY_INTERRUPTED, false))
        return Result.success()
    }

    companion object {
        private const val KEY_TRIP_ID = "trip_id"
        private const val KEY_INTERRUPTED = "interrupted"

        fun enqueue(context: Context, tripId: Long, interrupted: Boolean) {
            val request = OneTimeWorkRequestBuilder<FinalizeTripWorker>()
                .setInputData(workDataOf(KEY_TRIP_ID to tripId, KEY_INTERRUPTED to interrupted))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("finalize-trip-$tripId", ExistingWorkPolicy.KEEP, request)
        }
    }
}
