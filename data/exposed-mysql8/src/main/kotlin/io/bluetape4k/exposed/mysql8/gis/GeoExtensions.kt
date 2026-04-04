package io.bluetape4k.exposed.mysql8.gis

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
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

/**
 * MySQL 8.0 POINT 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object PlaceTable: LongIdTable("places") {
 *     val location = geoPoint("location")
 * }
 * val point = wgs84Point(126.9779, 37.5665)  // 서울 시청 (경도, 위도)
 * val id = PlaceTable.insertAndGetId { it[location] = point }
 * // PlaceTable.location.name == "location"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [Point] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoPoint(name: String, srid: Int = SRID_WGS84): Column<Point> {
    check(currentDialect is MysqlDialect) { "geoPoint는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, pointColumnType(srid))
}

/**
 * MySQL 8.0 POLYGON 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object ZoneTable: LongIdTable("zones") {
 *     val area = geoPolygon("area")
 * }
 * // ZoneTable.area.name == "area"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [Polygon] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoPolygon(name: String, srid: Int = SRID_WGS84): Column<Polygon> {
    check(currentDialect is MysqlDialect) { "geoPolygon는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, polygonColumnType(srid))
}

/**
 * MySQL 8.0 LINESTRING 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object RouteTable: LongIdTable("routes") {
 *     val path = geoLineString("path")
 * }
 * // RouteTable.path.name == "path"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [LineString] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoLineString(name: String, srid: Int = SRID_WGS84): Column<LineString> {
    check(currentDialect is MysqlDialect) { "geoLineString는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, lineStringColumnType(srid))
}

/**
 * MySQL 8.0 MULTIPOINT 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object ClusterTable: LongIdTable("clusters") {
 *     val points = geoMultiPoint("points")
 * }
 * // ClusterTable.points.name == "points"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [MultiPoint] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoMultiPoint(name: String, srid: Int = SRID_WGS84): Column<MultiPoint> {
    check(currentDialect is MysqlDialect) { "geoMultiPoint는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, multiPointColumnType(srid))
}

/**
 * MySQL 8.0 MULTIPOLYGON 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object RegionTable: LongIdTable("regions") {
 *     val zones = geoMultiPolygon("zones")
 * }
 * // RegionTable.zones.name == "zones"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [MultiPolygon] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoMultiPolygon(name: String, srid: Int = SRID_WGS84): Column<MultiPolygon> {
    check(currentDialect is MysqlDialect) { "geoMultiPolygon는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, multiPolygonColumnType(srid))
}

/**
 * MySQL 8.0 MULTILINESTRING 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object NetworkTable: LongIdTable("network") {
 *     val lines = geoMultiLineString("lines")
 * }
 * // NetworkTable.lines.name == "lines"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [MultiLineString] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoMultiLineString(name: String, srid: Int = SRID_WGS84): Column<MultiLineString> {
    check(currentDialect is MysqlDialect) { "geoMultiLineString는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, multiLineStringColumnType(srid))
}

/**
 * MySQL 8.0 GEOMETRY 범용 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object ShapeTable: LongIdTable("shapes") {
 *     val shape = geoGeometry("shape")
 * }
 * // ShapeTable.shape.name == "shape"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [Geometry] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoGeometry(name: String, srid: Int = SRID_WGS84): Column<Geometry> {
    check(currentDialect is MysqlDialect) { "geoGeometry는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, geometryColumnType(srid))
}

/**
 * MySQL 8.0 GEOMETRYCOLLECTION 컬럼을 테이블에 등록한다.
 *
 * ```kotlin
 * object MapTable: LongIdTable("maps") {
 *     val features = geoGeometryCollection("features")
 * }
 * // MapTable.features.name == "features"
 * ```
 *
 * @param name 컬럼 이름
 * @param srid SRID (기본값: 4326 WGS84)
 * @return [GeometryCollection] 타입의 [Column]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Table.geoGeometryCollection(name: String, srid: Int = SRID_WGS84): Column<GeometryCollection> {
    check(currentDialect is MysqlDialect) { "geoGeometryCollection는 MySQL dialect에서만 지원됩니다." }
    return registerColumn(name, geometryCollectionColumnType(srid))
}
