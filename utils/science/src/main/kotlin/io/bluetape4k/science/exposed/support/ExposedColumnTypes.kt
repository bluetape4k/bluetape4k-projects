package io.bluetape4k.science.exposed.support

import net.postgis.jdbc.PGgeometry
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

// ── PostGIS geometry columns ──────────────────────────────────────────────────

open class GeometryColumnType(private val geometryType: String = "") : ColumnType<PGgeometry>() {
    override fun sqlType() = if (geometryType.isBlank()) "GEOMETRY" else "GEOMETRY($geometryType, 4326)"

    override fun valueFromDB(value: Any): PGgeometry = when (value) {
        is PGgeometry -> value
        is String -> PGgeometry(value)
        else -> error("Unexpected geometry value: $value (${value::class.qualifiedName})")
    }

    override fun notNullValueToDB(value: PGgeometry): Any = value
}

fun Table.geoPoint(name: String): Column<PGgeometry> = registerColumn(name, GeometryColumnType("POINT"))
fun Table.geoPolygon(name: String): Column<PGgeometry> = registerColumn(name, GeometryColumnType("POLYGON"))
fun Table.geoGeometry(name: String): Column<PGgeometry> = registerColumn(name, GeometryColumnType())
