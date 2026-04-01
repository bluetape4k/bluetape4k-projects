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
     * @param value DB에서 읽은 값 ([PGgeometry] 또는 문자열)
     * @return 파싱된 [Point] 객체
     */
    override fun valueFromDB(value: Any): Point = when (value) {
        is PGgeometry -> value.geometry as Point
        is Point      -> value
        is String     -> PGgeometry(value).geometry as Point
        else          -> error("Unsupported value type: ${value::class.java}")
    }
}

/**
 * PostGIS POLYGON 타입을 저장하는 컬럼 타입.
 *
 * PostgreSQL + PostGIS 확장이 활성화된 환경에서만 사용 가능하다.
 * SRID 4326 (WGS 84) 좌표계를 사용한다.
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
     * @param value DB에서 읽은 값 ([PGgeometry] 또는 문자열)
     * @return 파싱된 [Polygon] 객체
     */
    override fun valueFromDB(value: Any): Polygon = when (value) {
        is PGgeometry -> value.geometry as Polygon
        is Polygon    -> value
        is String     -> PGgeometry(value).geometry as Polygon
        else          -> error("Unsupported value type: ${value::class.java}")
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
