package com.aijones.mileage.feature.insights.domain

class FakeShareCardWriter : ShareCardWriter {
    val saved = mutableMapOf<String, ByteArray>()

    override suspend fun save(png: ByteArray, name: String): String {
        saved[name] = png
        return "/cache/share/$name"
    }
}
