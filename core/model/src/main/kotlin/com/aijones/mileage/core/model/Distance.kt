package com.aijones.mileage.core.model

@JvmInline
value class Distance(val meters: Double) : Comparable<Distance> {
    val miles: Double get() = meters / METERS_PER_MILE
    val tenthsOfMile: Long get() = Math.round(miles * 10)
    operator fun plus(other: Distance) = Distance(meters + other.meters)
    operator fun times(factor: Int) = Distance(meters * factor)
    override fun compareTo(other: Distance) = meters.compareTo(other.meters)

    companion object {
        const val METERS_PER_MILE = 1609.344
        val ZERO = Distance(0.0)
        fun ofMiles(miles: Double) = Distance(miles * METERS_PER_MILE)
    }
}

fun mileageAmount(distance: Distance, ratePerMile: Money): Money =
    Money(Math.round(distance.tenthsOfMile * ratePerMile.micros / 10.0))
