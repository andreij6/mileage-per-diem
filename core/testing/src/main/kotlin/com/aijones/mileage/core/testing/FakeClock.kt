package com.aijones.mileage.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeClock(private var now: Instant = Instant.parse("2026-11-16T14:41:00Z"), private val zone: ZoneId = ZoneId.of("America/New_York")) :
    Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = FakeClock(now, zone)
    override fun instant(): Instant = now
    fun advance(by: Duration) {
        now = now.plus(by)
    }
    fun set(instant: Instant) {
        now = instant
    }
}
