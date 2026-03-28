package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import kotlin.reflect.KClass

/**
 * MySQL 8.0 Geometry 컬럼 타입.
 *
 * MySQL Internal Format (4바이트 LE SRID + WKB)으로 직렬화/역직렬화한다.
 *
 * @param T JTS Geometry 서브타입
 * @param geometryType MySQL SQL 타입 문자열 (예: "POINT", "POLYGON")
 * @param srid SRID (기본값: 4326)
 * @param geometryClass 런타임 타입 검증용 KClass
 */
class GeometryColumnType<T: Geometry>(
    private val geometryType: String,
    private val srid: Int = SRID_WGS84,
    private val geometryClass: KClass<out Geometry>,
): ColumnType<T>() {

    companion object: KLogging() {
        /** geometryType 문자열 -> KClass 매핑 (소문자 키) */
        private val GEOMETRY_TYPE_MAP: Map<String, KClass<out Geometry>> = mapOf(
            "point" to Point::class,
            "polygon" to Polygon::class,
            "linestring" to LineString::class,
            "multipoint" to MultiPoint::class,
            "multipolygon" to MultiPolygon::class,
            "multilinestring" to MultiLineString::class,
            "geometry" to Geometry::class,
            "geometrycollection" to GeometryCollection::class,
        )
    }

    init {
        geometryType.requireNotBlank("geometryType")
        srid.requirePositiveNumber("srid")
    }

    override fun sqlType(): String {
        check(currentDialect is MysqlDialect) {
            "GeometryColumnType은 MySQL dialect에서만 지원됩니다."
        }
        return "$geometryType SRID $srid"
    }

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T {
        val geometry = when (value) {
            is ByteArray -> MySqlWkbUtils.parseMySqlInternalGeometry(value)
            is Geometry -> value
            else -> error("지원하지 않는 타입: ${value::class.simpleName}")
        }
        // "GEOMETRY" 범용 컬럼은 타입 검증 건너뜀
        if (geometryType.uppercase() != "GEOMETRY") {
            val expectedClass = GEOMETRY_TYPE_MAP[geometryType.lowercase()]
                ?: error("알 수 없는 geometry 타입: $geometryType")
            check(expectedClass.isInstance(geometry)) {
                "Expected $geometryType but got ${geometry.geometryType}"
            }
        }
        return geometry as T
    }

    override fun notNullValueToDB(value: T): Any =
        MySqlWkbUtils.buildMySqlInternalGeometry(value, srid)

    override fun nonNullValueToString(value: T): String {
        val hex = MySqlWkbUtils.buildMySqlInternalGeometry(value, srid)
            .joinToString("") { "%02X".format(it) }
        return "ST_GeomFromWKB(X'$hex', $srid, 'axis-order=long-lat')"
    }
}

/** POINT 컬럼 타입 */
fun pointColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<Point>("POINT", srid, Point::class)

/** POLYGON 컬럼 타입 */
fun polygonColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<Polygon>("POLYGON", srid, Polygon::class)

/** LINESTRING 컬럼 타입 */
fun lineStringColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<LineString>("LINESTRING", srid, LineString::class)

/** MULTIPOINT 컬럼 타입 */
fun multiPointColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<MultiPoint>("MULTIPOINT", srid, MultiPoint::class)

/** MULTIPOLYGON 컬럼 타입 */
fun multiPolygonColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<MultiPolygon>("MULTIPOLYGON", srid, MultiPolygon::class)

/** MULTILINESTRING 컬럼 타입 */
fun multiLineStringColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<MultiLineString>("MULTILINESTRING", srid, MultiLineString::class)

/** GEOMETRY 범용 컬럼 타입 */
fun geometryColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<Geometry>("GEOMETRY", srid, Geometry::class)

/** GEOMETRYCOLLECTION 컬럼 타입 */
fun geometryCollectionColumnType(srid: Int = SRID_WGS84) =
    GeometryColumnType<GeometryCollection>("GEOMETRYCOLLECTION", srid, GeometryCollection::class)
