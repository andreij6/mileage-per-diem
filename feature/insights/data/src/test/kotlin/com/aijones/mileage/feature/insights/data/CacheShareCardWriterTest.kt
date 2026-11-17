package com.aijones.mileage.feature.insights.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CacheShareCardWriterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun savesPngInCacheShareReturnsContentUriAndOverwritesSameName() = runTest {
        val writer = CacheShareCardWriter(context, StandardTestDispatcher(testScheduler))
        val file = File(context.cacheDir, "share/year-in-review-3.png")

        val uri = writer.save(byteArrayOf(1, 2, 3, 4), "year-in-review-3.png")

        assertContentEquals(byteArrayOf(1, 2, 3, 4), file.readBytes())
        assertEquals("content://${context.packageName}.files/share/year-in-review-3.png", uri)

        writer.save(byteArrayOf(7, 7), "year-in-review-3.png")

        assertContentEquals(byteArrayOf(7, 7), file.readBytes())
        assertEquals(listOf("year-in-review-3.png"), file.parentFile!!.list()!!.toList())
    }
}
