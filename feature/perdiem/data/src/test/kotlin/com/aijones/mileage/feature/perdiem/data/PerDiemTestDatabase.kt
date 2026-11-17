package com.aijones.mileage.feature.perdiem.data

import android.content.Context
import androidx.room.Room

fun seededPerDiemDatabase(context: Context): PerDiemDatabase {
    val db = Room.inMemoryDatabaseBuilder(context, PerDiemDatabase::class.java).allowMainThreadQueries().build()
    val sql = db.openHelper.writableDatabase
    listOf(
        "(1, 2027, 'Standard rate', 'CONUS', NULL, 1, 6800)",
        "(2, 2027, 'New York City', 'NY', 'Bronx / Kings / New York / Queens / Richmond', 0, 9200)",
        "(3, 2027, 'Boston / Cambridge', 'MA', 'Suffolk, city of Cambridge', 0, 9200)",
        "(4, 2027, 'Albany', 'NY', 'Albany', 0, 8600)",
    ).forEach { sql.execSQL("INSERT INTO location (id, fy, city, state, county, isStandard, mieTotalCents) VALUES $it") }
    val lodging = mapOf(
        1L to List(12) { 11_300 },
        2L to listOf(18_900, 18_900, 29_600, 29_600, 29_600, 29_600, 25_000, 25_000, 35_800, 35_800, 35_800, 35_800),
        3L to List(12) { 21_300 },
        4L to List(12) { 13_300 },
    )
    lodging.forEach { (id, months) -> months.forEachIndexed { i, cents -> sql.execSQL("INSERT INTO lodging VALUES ($id, ${i + 1}, $cents)") } }
    listOf("6800, 1600, 1900, 2800, 500, 5100", "8600, 2200, 2300, 3600, 500, 6450", "9200, 2300, 2600, 3800, 500, 6900")
        .forEach { sql.execSQL("INSERT INTO mie VALUES (2027, $it)") }
    return db
}
