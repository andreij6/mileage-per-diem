package com.aijones.mileage.feature.tracking.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.feature.tracking.domain.PlaceLabeler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

class GeocoderPlaceLabeler @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PlaceLabeler {
    override suspend fun label(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = withTimeoutOrNull(TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(point.lat, point.lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                        override fun onError(errorMessage: String?) = cont.resume(null)
                    })
                }
            } else {
                withContext(io) {
                    @Suppress("DEPRECATION")
                    runCatching { geocoder.getFromLocation(point.lat, point.lng, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
        return address?.let { shortLabel(it.featureName, it.thoroughfare, it.subLocality ?: it.locality) }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}

internal fun shortLabel(feature: String?, thoroughfare: String?, area: String?): String? {
    val name = feature?.trim()?.takeUnless { it.isEmpty() || it.first().isDigit() } ?: thoroughfare?.trim()
    return listOfNotNull(name, area?.trim()).filter { it.isNotEmpty() }.distinct().joinToString(", ").ifEmpty { null }
}
