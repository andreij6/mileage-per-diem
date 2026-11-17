package com.aijones.mileage.feature.perdiem.presentation

import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.groupThousands
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal const val UNKNOWN_LOCATION_ID = -1L

internal val FISCAL_MONTHS: List<Month> = List(12) { Month.OCTOBER.plus(it.toLong()) }

internal fun Money.formatRate(): String = if (cents % 100 == 0L) "$" + groupThousands(cents / 100) else formatUsd()

internal fun Month.shortName(locale: Locale = Locale.getDefault()): String = getDisplayName(TextStyle.SHORT, locale)

internal fun Month.fullName(locale: Locale = Locale.getDefault()): String = getDisplayName(TextStyle.FULL, locale)

internal fun Month.initial(locale: Locale = Locale.getDefault()): String = getDisplayName(TextStyle.NARROW, locale)

internal fun LocalDate.formatStayDate(locale: Locale = Locale.getDefault()): String = DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(this)
