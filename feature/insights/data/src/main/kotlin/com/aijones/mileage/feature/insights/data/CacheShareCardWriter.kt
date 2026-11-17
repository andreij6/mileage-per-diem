package com.aijones.mileage.feature.insights.data

import android.content.Context
import androidx.core.content.FileProvider
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.feature.insights.domain.ShareCardWriter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CacheShareCardWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ShareCardWriter {
    override suspend fun save(png: ByteArray, name: String): String = withContext(io) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, File(name).name)
        file.writeBytes(png)
        FileProvider.getUriForFile(context, context.packageName + ".files", file).toString()
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface InsightsDataModule {
    @Binds
    fun shareCardWriter(impl: CacheShareCardWriter): ShareCardWriter
}
