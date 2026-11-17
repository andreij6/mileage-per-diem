package com.aijones.mileage.core.model

@JvmInline
value class Money(val micros: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(micros + other.micros)
    operator fun minus(other: Money) = Money(micros - other.micros)
    operator fun times(factor: Int) = Money(micros * factor)
    override fun compareTo(other: Money) = micros.compareTo(other.micros)

    val cents: Long get() = Math.floorDiv(micros + 5_000, 10_000)

    fun percent(pct: Int): Money = Money(Math.floorDiv(micros * pct + 50, 100))

    companion object {
        val ZERO = Money(0)
        fun ofCents(cents: Long) = Money(cents * 10_000)
        fun ofDollars(dollars: Long) = Money(dollars * 1_000_000)
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { acc, m -> acc + m }
