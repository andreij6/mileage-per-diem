package com.aijones.mileage.core.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lng: Double)

fun GeoPoint.distanceTo(other: GeoPoint): Distance {
    val dLat = Math.toRadians(other.lat - lat)
    val dLng = Math.toRadians(other.lng - lng)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLng / 2).pow(2)
    return Distance(2 * EARTH_RADIUS_M * asin(sqrt(a)))
}

private const val EARTH_RADIUS_M = 6_371_008.8
