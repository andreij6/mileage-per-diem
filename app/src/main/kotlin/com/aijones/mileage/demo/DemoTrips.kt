package com.aijones.mileage.demo

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class DemoPlace(val label: String, val point: GeoPoint)

object Places {
    val office = DemoPlace("Office", GeoPoint(40.7033, -73.9881))
    val acme = DemoPlace("Acme HQ", GeoPoint(40.7536, -73.9832))
    val jfk = DemoPlace("JFK Airport", GeoPoint(40.6413, -73.7781))
    val home = DemoPlace("Home", GeoPoint(40.6782, -73.9442))
    val newark = DemoPlace("Newark Supply", GeoPoint(40.7357, -74.1724))
    val stamford = DemoPlace("Stamford", GeoPoint(41.0534, -73.5387))
    val all = listOf(office, acme, jfk, home, newark, stamford)
}

data class DemoTrip(
    val from: DemoPlace,
    val to: DemoPlace,
    val start: ZonedDateTime,
    val minutes: Long,
    val miles: Double,
    val purpose: String?,
    val needsReview: Boolean = false,
)

object DemoTrips {
    val zone: ZoneId = ZoneId.of("America/New_York")
    private fun at(date: String, time: String) = ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), zone)

    fun november(): List<DemoTrip> {
        val fixed = listOf(
            DemoTrip(Places.office, Places.acme, at("2026-11-16", "09:12"), 29, 18.4, "Client meeting"),
            DemoTrip(Places.acme, Places.office, at("2026-11-16", "15:02"), 36, 19.1, "Client meeting"),
            DemoTrip(Places.office, Places.jfk, at("2026-11-15", "06:40"), 42, 16.2, "Travel"),
            DemoTrip(Places.home, Places.newark, at("2026-11-13", "11:05"), 46, 24.8, "Site visit"),
            DemoTrip(Places.newark, Places.home, at("2026-11-13", "14:20"), 42, 24.6, "Site visit"),
            DemoTrip(Places.jfk, Places.stamford, at("2026-11-10", "08:10"), 55, 41.2, "Client meeting"),
        )
        val rnd = Random(11)
        val days = listOf(2, 3, 4, 6, 9, 10, 11, 12, 12, 2, 4, 6, 9, 10, 11, 3, 4, 6, 9, 10, 11, 12, 2, 4, 10, 9)
        val target = 845.0 - fixed.sumOf { it.miles }
        val weights = days.map { 0.6 + rnd.nextDouble() }
        val scale = target / weights.sum()
        val generated = days.mapIndexed { i, d ->
            val miles = Math.round(weights[i] * scale * 10) / 10.0
            val (a, b) = Places.all.shuffled(rnd).take(2)
            DemoTrip(a, b, at("2026-11-%02d".format(d), "%02d:%02d".format(8 + i % 9, (i * 7) % 60)), (miles * 2.1).toLong() + 6, miles, "Client meeting")
        }
        val fix = Math.round((845.0 - fixed.sumOf { it.miles } - generated.sumOf { it.miles }) * 10) / 10.0
        val adjusted = generated.dropLast(1) + generated.last().let { it.copy(miles = Math.round((it.miles + fix) * 10) / 10.0) }
        val review = listOf(
            DemoTrip(Places.home, Places.newark, at("2026-11-12", "11:05"), 46, 24.8, null, needsReview = true),
            DemoTrip(Places.office, Places.acme, at("2026-11-11", "17:30"), 31, 17.9, null, needsReview = true),
            DemoTrip(Places.home, Places.jfk, at("2026-11-08", "07:15"), 38, 15.6, null, needsReview = true),
        )
        return fixed + adjusted + review
    }

    fun earlierThisYear(): List<DemoTrip> {
        val rnd = Random(2026)
        val h1Target = 4_980.0
        val h2Target = 4_432.0 - 845.0
        fun spread(months: IntRange, target: Double, count: Int): List<DemoTrip> {
            val weights = List(count) { 0.5 + rnd.nextDouble() }
            val scale = target / weights.sum()
            val trips = weights.mapIndexed { i, w ->
                val month = months.first + i * (months.last - months.first + 1) / count
                val day = 1 + rnd.nextInt(27)
                val (a, b) = if (i % 3 == 0) Places.office to Places.acme else Places.all.shuffled(rnd).take(2).let { it[0] to it[1] }
                val miles = Math.round(w * scale * 10) / 10.0
                DemoTrip(a, b, ZonedDateTime.of(LocalDate.of(2026, month, day), LocalTime.of(8 + i % 10, (i * 13) % 60), zone), (miles * 2).toLong() + 5, miles, "Client meeting")
            }
            val diff = Math.round((target - trips.sumOf { it.miles }) * 10) / 10.0
            return trips.dropLast(1) + trips.last().let { it.copy(miles = Math.round((it.miles + diff) * 10) / 10.0) }
        }
        return spread(1..6, h1Target, 110) + spread(7..10, h2Target, 70)
    }

    fun track(trip: DemoTrip, points: Int = 24): List<Pair<GeoPoint, Long>> {
        val rnd = Random(trip.start.toEpochSecond())
        val bend = (rnd.nextDouble() - 0.5) * 0.02
        val startMs = trip.start.toInstant().toEpochMilli()
        val durationMs = trip.minutes * 60_000
        return List(points) { i ->
            val f = i / (points - 1.0)
            val lat = trip.from.point.lat + (trip.to.point.lat - trip.from.point.lat) * f + bend * sin(PI * f)
            val lng = trip.from.point.lng + (trip.to.point.lng - trip.from.point.lng) * f + bend * sin(2 * PI * f) / 2
            GeoPoint(lat, lng) to startMs + (durationMs * f).toLong()
        }
    }

    fun distance(trip: DemoTrip) = Distance.ofMiles(trip.miles)
}
