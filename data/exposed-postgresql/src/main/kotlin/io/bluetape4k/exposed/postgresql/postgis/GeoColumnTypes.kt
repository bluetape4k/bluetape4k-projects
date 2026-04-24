package io.bluetape4k.exposed.postgresql.postgis

import io.bluetape4k.logging.KLogging
import net.postgis.jdbc.PGgeometry
import net.postgis.jdbc.geometry.Geometry
import net.postgis.jdbc.geometry.Point
import net.postgis.jdbc.geometry.Polygon
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * PostGIS POINT 타입을 저장하는 컬럼 타입.
 *
 * PostgreSQL + PostGIS 확장이 활성화된 환경에서만 사용 가능하다.
 * SRID 4326 (WGS 84) 좌표계를 사용한다.
 *
 * 좌표 순서: `Point(x=경도, y=위도)`
 *
 * ```kotlin
 * object PlaceTable: LongIdTable("places") {
 *     val location = geoPoint("location")
 * }
 * val point = Point(126.9779, 37.5665)  // 서울 시청 (경도, 위도)
 * point.srid = 4326
 * val id = PlaceTable.insertAndGetId { it[location] = point }
 * val row = PlaceTable.selectAll().where { PlaceTable.id eq id }.single()
 * // row[PlaceTable.location].x == 126.9779
 * ```
 */
class GeoPointColumnType: ColumnType<Point>() {

    companion object: KLogging()

    /**
     * DB SQL 타입을 반환한다.
     *
     * @return `"GEOMETRY(POINT, 4326)"`
     * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
     */
    override fun sqlType(): String {
        check(currentDialect is PostgreSQLDialect) {
            "GeoPointColumnType 은 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
        }
        return "GEOMETRY(POINT, 4326)"
    }

    /**
     * [Point] 값을 DB에 저장할 형태로 변환한다.
     *
     * @param value 저장할 [Point] 객체
     * @return [PGgeometry] 래퍼 객체
     */
    override fun notNullValueToDB(value: Point): Any {
        if (value.srid == Point.UNKNOWN_SRID) {
            value.srid = SRID_WGS84
        }
        return PGgeometry(value)
    }

    /**
     * DB에서 읽은 값을 [Point]로 변환한다.
     *
     * geometry 타입이 [Point]가 아닌 경우 [IllegalStateException]을 던진다.
     * DB 스키마와 컬럼 타입이 맞지 않을 때 빠르게 문제를 인지할 수 있도록 한다.
     *
     * @param value DB에서 읽은 값 ([PGgeometry] 또는 문자열)
     * @return 파싱된 [Point] 객체
     * @throws IllegalStateException geometry 타입이 Point가 아닌 경우
     */
    override fun valueFromDB(value: Any): Point = when (value) {
        is PGgeometry -> {
            val geom = value.geometry
            // PGgeometry.geometry 는 내부적으로 null 을 반환할 수 있으며,
            // 이후 'as Point' 비검사 캐스트를 제거하고 명확한 타입 검증으로 교체한다.
            // DB 스키마 불일치(예: POLYGON 컬럼을 Point 로 읽는 경우)를 즉시 인지하기 위함이다.
            checkNotNull(geom) { "PGgeometry.geometry 가 null 입니다." }
            check(geom is Point) {
                "GeoPointColumnType: geometry 타입이 Point 가 아닙니다. 실제 타입: ${geom::class.java.simpleName}"
            }
            geom
        }
        is Point      -> value
        is String     -> {
            val geom = PGgeometry(value).geometry
            // 문자열 파싱 후에도 동일하게 검증: WKT 문자열이 POLYGON 등 다른 타입을 나타낼 수 있다.
            checkNotNull(geom) { "PGgeometry.geometry 가 null 입니다: '$value'" }
            check(geom is Point) {
                "GeoPointColumnType: geometry 타입이 Point 가 아닙니다. 실제 타입: ${geom::class.java.simpleName}"
            }
            geom
        }
        else          -> error("GeoPointColumnType: 지원하지 않는 값 타입입니다: ${value::class.java}")
    }
}

/**
 * PostGIS POLYGON 타입을 저장하는 컬럼 타입.
 *
 * PostgreSQL + PostGIS 확장이 활성화된 환경에서만 사용 가능하다.
 * SRID 4326 (WGS 84) 좌표계를 사용한다.
 *
 * ```kotlin
 * object ZoneTable: LongIdTable("zones") {
 *     val area = geoPolygon("area")
 * }
 * // ZoneTable.area.columnType is GeoPolygonColumnType
 * ```
 */
class GeoPolygonColumnType: ColumnType<Polygon>() {

    companion object: KLogging()

    /**
     * DB SQL 타입을 반환한다.
     *
     * @return `"GEOMETRY(POLYGON, 4326)"`
     * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
     */
    override fun sqlType(): String {
        check(currentDialect is PostgreSQLDialect) {
            "GeoPolygonColumnType 은 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
        }
        return "GEOMETRY(POLYGON, 4326)"
    }

    /**
     * [Polygon] 값을 DB에 저장할 형태로 변환한다.
     *
     * @param value 저장할 [Polygon] 객체
     * @return [PGgeometry] 래퍼 객체
     */
    override fun notNullValueToDB(value: Polygon): Any {
        if (value.srid == Polygon.UNKNOWN_SRID) {
            value.srid = SRID_WGS84
        }
        return PGgeometry(value)
    }

    /**
     * DB에서 읽은 값을 [Polygon]으로 변환한다.
     *
     * geometry 타입이 [Polygon]이 아닌 경우 [IllegalStateException]을 던진다.
     * DB 스키마와 컬럼 타입이 맞지 않을 때 빠르게 문제를 인지할 수 있도록 한다.
     *
     * @param value DB에서 읽은 값 ([PGgeometry] 또는 문자열)
     * @return 파싱된 [Polygon] 객체
     * @throws IllegalStateException geometry 타입이 Polygon이 아닌 경우
     */
    override fun valueFromDB(value: Any): Polygon = when (value) {
        is PGgeometry -> {
            val geom = value.geometry
            // 이전 코드의 'as Polygon' 비검사 캐스트를 제거하고 명시적 검증으로 교체한다.
            // DB 스키마와 컬럼 매핑이 불일치할 때(예: POINT 컬럼을 Polygon 으로 읽는 경우)
            // ClassCastException 대신 의미 있는 오류 메시지로 빠르게 실패한다.
            checkNotNull(geom) { "PGgeometry.geometry 가 null 입니다." }
            check(geom is Polygon) {
                "GeoPolygonColumnType: geometry 타입이 Polygon 이 아닙니다. 실제 타입: ${geom::class.java.simpleName}"
            }
            geom
        }
        is Polygon    -> value
        is String     -> {
            val geom = PGgeometry(value).geometry
            // 문자열 파싱 후에도 동일하게 검증: WKT 문자열이 POINT 등 다른 타입을 나타낼 수 있다.
            checkNotNull(geom) { "PGgeometry.geometry 가 null 입니다: '$value'" }
            check(geom is Polygon) {
                "GeoPolygonColumnType: geometry 타입이 Polygon 이 아닙니다. 실제 타입: ${geom::class.java.simpleName}"
            }
            geom
        }
        else          -> error("GeoPolygonColumnType: 지원하지 않는 값 타입입니다: ${value::class.java}")
    }
}

/**
 * PostGIS의 모든 Geometry 타입(POINT, POLYGON, LINESTRING, MULTIPOLYGON 등)을 수용하는
 * generic Geometry 컬럼 타입.
 *
 * 특정 하위 타입에 구애받지 않고 다양한 geometry를 하나의 컬럼에 저장할 수 있다.
 * SRID 4326 (WGS 84) 좌표계를 사용한다.
 *
 * SQL 타입: `GEOMETRY(GEOMETRY, 4326)`
 *
 * ```kotlin
 * object ShapeTable: LongIdTable("shapes") {
 *     val shape = geoGeometry("shape")
 * }
 * // ShapeTable.shape.columnType is GeoGeometryColumnType
 * ```
 */
class GeoGeometryColumnType: ColumnType<Geometry>() {

    companion object: KLogging()

    /**
     * DB SQL 타입을 반환한다.
     *
     * @return `"GEOMETRY(GEOMETRY, 4326)"`
     * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
     */
    override fun sqlType(): String {
        check(currentDialect is PostgreSQLDialect) {
            "GeoGeometryColumnType 은 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
        }
        return "GEOMETRY(GEOMETRY, 4326)"
    }

    /**
     * [Geometry] 값을 DB에 저장할 형태로 변환한다.
     *
     * @param value 저장할 [Geometry] 객체
     * @return [PGgeometry] 래퍼 객체
     */
    override fun notNullValueToDB(value: Geometry): Any {
        if (value.srid == Geometry.UNKNOWN_SRID) {
            value.srid = SRID_WGS84
        }
        return PGgeometry(value)
    }

    /**
     * DB에서 읽은 값을 [Geometry]로 변환한다.
     *
     * @param value DB에서 읽은 값 ([PGgeometry] 또는 문자열)
     * @return 파싱된 [Geometry] 객체
     */
    override fun valueFromDB(value: Any): Geometry = when (value) {
        is PGgeometry -> value.geometry
        is Geometry   -> value
        is String     -> PGgeometry(value).geometry
        else          -> error("Unsupported value type: ${value::class.java}")
    }
}

/** WGS 84 좌표계 SRID */
private const val SRID_WGS84 = 4326
