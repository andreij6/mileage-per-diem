package com.aijones.mileage.feature.perdiem.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.aijones.mileage.core.database.MileageDatabase
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.Stay
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomStayRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MileageDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = RoomStayRepository(db.stayDao(), Dispatchers.Unconfined)

    @After
    fun close() = db.close()

    private fun stay(checkIn: String, checkOut: String) = Stay(
        locationId = 2,
        locationLabel = "New York City, NY",
        fiscalYear = 2027,
        checkIn = LocalDate.parse(checkIn),
        checkOut = LocalDate.parse(checkOut),
        includeLodging = true,
        mieDaily = Money.ofDollars(92),
        mieTotal = Money.ofDollars(690),
        lodgingTotal = Money.ofCents(250_600),
    )

    @Test
    fun `stays round-trip and are observed when they overlap the range`() = runTest {
        val november = repository.add(stay("2026-11-18", "2026-11-25"))
        repository.add(stay("2026-10-30", "2026-11-02"))
        repository.add(stay("2026-12-03", "2026-12-04"))

        repository.observeOverlapping(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")).test {
            val stays = awaitItem()
            assertEquals(listOf(LocalDate.parse("2026-10-30"), LocalDate.parse("2026-11-18")), stays.map { it.checkIn })
            assertEquals(stay("2026-11-18", "2026-11-25").copy(id = november), stays[1])
            assertEquals(Money.ofCents(319_600), stays[1].total)

            repository.delete(november)
            assertEquals(1, awaitItem().size)
        }
    }
}
