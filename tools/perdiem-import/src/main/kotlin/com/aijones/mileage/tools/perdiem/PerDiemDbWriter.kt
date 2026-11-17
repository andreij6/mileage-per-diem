package com.aijones.mileage.tools.perdiem

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun roomSchemaStatements(roomSchemaJson: String): List<String> {
    val database = Json.parseToJsonElement(roomSchemaJson).jsonObject.getValue("database").jsonObject
    val tables = database.getValue("entities").jsonArray.flatMap { element ->
        val entity = element.jsonObject
        val name = entity.getValue("tableName").jsonPrimitive.content
        val triggers = entity["contentSyncTriggers"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        listOf(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name)) + triggers
    }
    val setup = database.getValue("setupQueries").jsonArray.map { it.jsonPrimitive.content }
    return tables + setup + "PRAGMA user_version = ${database.getValue("version").jsonPrimitive.int}"
}

fun writePerDiemDb(data: ImportedYear, roomSchema: File, out: File) {
    out.parentFile?.mkdirs()
    out.delete()
    DriverManager.getConnection("jdbc:sqlite:${out.absolutePath}").use { connection ->
        connection.createStatement().use { st -> roomSchemaStatements(roomSchema.readText()).forEach(st::execute) }
        connection.autoCommit = false
        insert(connection, data)
        connection.commit()
        connection.autoCommit = true
        connection.createStatement().use { it.execute("INSERT INTO location_fts(location_fts) VALUES('optimize')") }
        connection.createStatement().use { it.execute("VACUUM") }
    }
}

private fun insert(connection: Connection, data: ImportedYear) {
    connection.prepareStatement("INSERT INTO location (id, fy, city, state, county, isStandard, mieTotalCents) VALUES (?, ?, ?, ?, ?, ?, ?)").use { st ->
        data.locations.forEach {
            st.setLong(1, it.id)
            st.setInt(2, data.fiscalYear)
            st.setString(3, it.city)
            st.setString(4, it.state)
            st.setString(5, it.county)
            st.setInt(6, if (it.isStandard) 1 else 0)
            st.setLong(7, it.mieTotalCents)
            st.addBatch()
        }
        st.executeBatch()
    }
    connection.prepareStatement("INSERT INTO lodging (locationId, month, cents) VALUES (?, ?, ?)").use { st ->
        data.locations.forEach { location ->
            location.lodgingCentsByMonth.forEach { (month, cents) ->
                st.setLong(1, location.id)
                st.setInt(2, month)
                st.setLong(3, cents)
                st.addBatch()
            }
        }
        st.executeBatch()
    }
    connection.prepareStatement(
        "INSERT INTO mie (fy, totalCents, breakfastCents, lunchCents, dinnerCents, incidentalsCents, firstLastCents) VALUES (?, ?, ?, ?, ?, ?, ?)",
    ).use { st ->
        data.mie.forEach {
            st.setInt(1, data.fiscalYear)
            st.setLong(2, it.totalCents)
            st.setLong(3, it.breakfastCents)
            st.setLong(4, it.lunchCents)
            st.setLong(5, it.dinnerCents)
            st.setLong(6, it.incidentalsCents)
            st.setLong(7, it.firstLastCents)
            st.addBatch()
        }
        st.executeBatch()
    }
}
