package com.aijones.mileage.feature.tracking.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.feature.tracking.domain.Fix
import com.aijones.mileage.feature.tracking.domain.LocationTracker
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class FusedLocationTracker @Inject constructor(@ApplicationContext private val context: Context) : LocationTracker {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    override fun fixes(policy: RecordingPolicy): Flow<Fix> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toFix()) }
            }
        }
        client.requestLocationUpdates(policy.toRequest(), callback, Looper.getMainLooper()).await()
        awaitClose { client.removeLocationUpdates(callback) }
    }
}

internal fun RecordingPolicy.toRequest(): LocationRequest =
    LocationRequest.Builder(if (highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
        .setMinUpdateDistanceMeters(minDistanceM)
        .build()

internal fun Location.toFix() = Fix(
    t = Instant.ofEpochMilli(time),
    point = GeoPoint(latitude, longitude),
    accuracyM = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    speedMps = if (hasSpeed()) speed else null,
)
