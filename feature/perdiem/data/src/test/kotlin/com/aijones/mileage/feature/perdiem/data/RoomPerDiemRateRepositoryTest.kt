package com.aijones.mileage.feature.perdiem.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.MieBreakdown
import com.aijones.mileage.feature.perdiem.domain.PerDiemLocation
import java.time.Month
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomPerDiemRateRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = seededPerDiemDatabase(context)
    private val repository = RoomPerDiemRateRepository({ db }, Dispatchers.Unconfined)

    @After
    fun close() = db.close()

    @Test
    fun `prefix search matches city county and state words, ordered by city, never the standard rate`() = runTest {
        assertEquals(listOf("Boston / Cambridge, MA"), repository.search("bos", 2027).map { it.label })
        assertEquals(listOf("Albany, NY", "New York City, NY"), repository.search("ny", 2027).map { it.label })
        assertEquals(listOf("New York City, NY"), repository.search("queens", 2027).map { it.label })
        assertEquals(listOf("New York City, NY"), repository.search("New Yo", 2027).map { it.label })
        assertTrue(repository.search("standard", 2027).isEmpty())
    }

    @Test
    fun `search ignores other fiscal years and punctuation-only queries`() = runTest {
        assertTrue(repository.search("bos", 2026).isEmpty())
        assertTrue(repository.search(" \"*' ", 2027).isEmpty())
    }

    @Test
    fun `rates map monthly lodging and the M&IE tier`() = runTest {
        val nyc = repository.rates(2)!!
        assertEquals(PerDiemLocation(2, "New York City", "NY", "Bronx / Kings / New York / Queens / Richmond", 2027, false), nyc.location)
        assertEquals(Money.ofDollars(358), nyc.lodgingByMonth[Month.NOVEMBER])
        assertEquals(Money.ofDollars(189), nyc.lodgingByMonth[Month.JANUARY])
        assertEquals(12, nyc.lodgingByMonth.size)
        assertEquals(
            MieBreakdown(Money.ofDollars(92), Money.ofDollars(23), Money.ofDollars(26), Money.ofDollars(38), Money.ofDollars(5), Money.ofDollars(69)),
            nyc.mie,
        )
    }

    @Test
    fun `unknown places fall back to the standard CONUS rate`() = runTest {
        val rates = repository.rates(999)!!
        assertTrue(rates.location.isStandard)
        assertEquals(Money.ofDollars(113), rates.lodgingByMonth[Month.MARCH])
        assertEquals(Money.ofCents(5_100), rates.mie.firstLastDay)
        assertEquals(rates, repository.standardRates(2027))
        assertEquals(rates, repository.standardRates(2031))
    }

    @Test
    fun `the bundled FY2027 asset opens through Room with GSA rates`() = runTest {
        val asset = Room.databaseBuilder(context, PerDiemDatabase::class.java, "asset-test.db").createFromAsset("perdiem.db").allowMainThreadQueries().build()
        val bundled = RoomPerDiemRateRepository({ asset }, Dispatchers.Unconfined)
        val standard = bundled.standardRates(2027)
        assertEquals(Money.ofDollars(113), standard.lodgingByMonth[Month.OCTOBER])
        assertEquals(Money.ofDollars(68), standard.mie.total)
        val nyc = bundled.search("new york", 2027).single { it.city == "New York City" }
        assertEquals(Money.ofDollars(92), bundled.rates(nyc.id)!!.mie.total)
        assertTrue(bundled.search("bos", 2027).any { it.city.startsWith("Boston") })
        asset.close()
    }
}
