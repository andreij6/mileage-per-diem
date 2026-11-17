package com.aijones.mileage.tools.perdiem

import com.aijones.mileage.tools.perdiem.gsa.infrastructure.Serializer
import com.aijones.mileage.tools.perdiem.gsa.models.LodgingRate
import com.aijones.mileage.tools.perdiem.gsa.models.MieRate
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PerDiemImportTest {
    private fun resource(name: String) = checkNotNull(javaClass.getResource("/$name")).readText()
    private val lodging = Serializer.kotlinxSerializationJson.decodeFromString<List<LodgingRate>>(resource("gsa-conus-lodging-2027.json"))
    private val mie = Serializer.kotlinxSerializationJson.decodeFromString<List<MieRate>>(resource("gsa-conus-mie-2027.json"))
    private val schema = File("../../feature/perdiem/data/schemas/com.aijones.mileage.feature.perdiem.data.PerDiemDatabase/1.json")

    @Test
    fun `standard rows collapse into one CONUS location at FY2027 rates`() {
        val year = mapGsa(2027, lodging, mie)
        val standard = year.locations.single { it.isStandard }
        assertEquals(ImportedLocation(1, "Standard rate", "CONUS", null, true, 6_800, (1..12).associateWith { 11_300L }), standard)
    }

    @Test
    fun `destinations get stable ids sorted by state then city`() {
        val labels = mapGsa(2027, lodging, mie).locations.drop(1).map { "${it.id} ${it.city}, ${it.state}" }
        assertEquals(
            listOf(
                "2 Birmingham, AL", "3 District of Columbia, DC", "4 Boston / Cambridge, MA",
                "5 District of Columbia, MD", "6 New York City, NY", "7 District of Columbia, VA",
            ),
            labels,
        )
    }

    @Test
    fun `monthly lodging and meals map to cents`() {
        val nyc = mapGsa(2027, lodging, mie).locations.single { it.city == "New York City" }
        assertEquals(9_200, nyc.mieTotalCents)
        assertEquals("Bronx / Kings / New York / Queens / Richmond", nyc.county)
        assertEquals(35_800, nyc.lodgingCentsByMonth[11])
        assertEquals(18_900, nyc.lodgingCentsByMonth[1])
        assertEquals(25_000, nyc.lodgingCentsByMonth[7])
    }

    @Test
    fun `mie tiers keep the 75 percent first and last day amounts`() {
        val tiers = mapGsa(2027, lodging, mie).mie
        assertEquals(listOf(6_800L, 7_400, 8_000, 8_600, 9_200), tiers.map { it.totalCents })
        assertEquals(ImportedMie(7_400, 1_800, 2_000, 3_100, 500, 5_550), tiers[1])
    }

    @Test
    fun `a meals amount without an M&IE tier is rejected`() {
        assertFailsWith<IllegalStateException> { mapGsa(2027, lodging, mie.filterNot { it.total == 92.0 }) }
    }

    @Test
    fun `writer builds a Room-compatible database with prefix search`() {
        val out = Files.createTempFile("perdiem", ".db").toFile().apply { deleteOnExit() }
        writePerDiemDb(mapGsa(2027, lodging, mie), schema, out)
        DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { c ->
            c.createStatement().use { st ->
                st.executeQuery("PRAGMA user_version").use { assertEquals(1, it.getInt(1)) }
                st.executeQuery("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
                    assertEquals(Regex("'([0-9a-f]+)'").find(schema.readText())!!.groupValues[1], it.getString(1))
                }
                st.executeQuery(
                    "SELECT location.city FROM location JOIN location_fts ON location.id = location_fts.rowid WHERE location_fts MATCH 'bos*'",
                ).use { rs ->
                    rs.next()
                    assertEquals("Boston / Cambridge", rs.getString(1))
                }
                st.executeQuery("SELECT COUNT(*) FROM lodging").use { assertEquals(7 * 12, it.getInt(1)) }
            }
        }
    }
}
