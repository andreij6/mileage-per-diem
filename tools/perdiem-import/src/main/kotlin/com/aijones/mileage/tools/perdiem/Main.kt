package com.aijones.mileage.tools.perdiem

import com.aijones.mileage.tools.perdiem.gsa.apis.RatesApi
import com.aijones.mileage.tools.perdiem.gsa.infrastructure.ApiClient
import java.io.File

private const val SCHEMA = "feature/perdiem/data/schemas/com.aijones.mileage.feature.perdiem.data.PerDiemDatabase/1.json"
private const val ASSET = "feature/perdiem/data/src/main/assets/perdiem.db"

fun main(args: Array<String>) {
    val year = args.getOrNull(0)?.toInt() ?: 2027
    val schema = File(args.getOrNull(1) ?: SCHEMA)
    val out = File(args.getOrNull(2) ?: ASSET)
    ApiClient.apiKey["X-API-KEY"] = System.getenv("GSA_API_KEY") ?: "DEMO_KEY"
    val api = RatesApi()
    val data = mapGsa(year, api.conusLodging(year.toString()), api.conusMie(year.toString()))
    writePerDiemDb(data, schema, out)
    val standard = data.locations.first { it.isStandard }
    println(
        "FY$year: ${data.locations.size} locations, ${data.mie.size} M&IE tiers, standard ${standard.lodgingCentsByMonth[10]?.div(100)} lodging / " +
            "${standard.mieTotalCents / 100} M&IE -> $out (${out.length()} bytes)",
    )
}
