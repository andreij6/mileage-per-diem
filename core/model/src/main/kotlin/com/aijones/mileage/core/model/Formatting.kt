package com.aijones.mileage.core.model

import kotlin.math.abs

fun Money.formatUsd(): String {
    val c = cents
    val sign = if (c < 0) "-" else ""
    val whole = abs(c) / 100
    val frac = abs(c) % 100
    return "$sign$" + groupThousands(whole) + "." + frac.toString().padStart(2, '0')
}

fun Money.formatCentsPerMile(): String {
    val tenthCents = Math.round(micros / 1_000.0)
    return if (tenthCents % 10 == 0L) "${tenthCents / 10}¢" else "${tenthCents / 10}.${tenthCents % 10}¢"
}

fun Distance.formatMiles(): String {
    val t = tenthsOfMile
    return groupThousands(t / 10) + "." + (t % 10) + " mi"
}

fun groupThousands(value: Long): String {
    val s = value.toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, ch ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(ch)
    }
    return sb.toString()
}
