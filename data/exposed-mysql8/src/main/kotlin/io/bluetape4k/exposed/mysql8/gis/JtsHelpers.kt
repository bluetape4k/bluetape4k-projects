package io.bluetape4k.exposed.mysql8.gis

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel

/**
 * WGS84(SRID 4326) 기준 JTS GeometryFactory 싱글턴.
 *
 * 좌표 순서 규약: **longitude(경도, X축) 먼저, latitude(위도, Y축) 두 번째**.
 * MySQL 8.0 SRID 4326에서 axis-order=long-lat으로 저장/조회된다.
 *
 * ```kotlin
 * val point = WGS84_FACTORY.createPoint(Coordinate(126.9779, 37.5665))
 * // point.srid == 4326
 * ```
 */
val WGS84_FACTORY: GeometryFactory = GeometryFactory(PrecisionModel(), SRID_WGS84)

/**
 * WGS84 Point를 생성한다.
 *
 * ```kotlin
 * val point = wgs84Point(126.9779, 37.5665)  // 서울 시청 (경도, 위도)
 * // point.x == 126.9779
 * // point.y == 37.5665
 * ```
 *
 * @param lng 경도 (X축, -180 ~ 180)
 * @param lat 위도 (Y축, -90 ~ 90)
 */
fun wgs84Point(lng: Double, lat: Double): Point =
    WGS84_FACTORY.createPoint(Coordinate(lng, lat))

/**
 * WGS84 Polygon을 생성한다.
 *
 * ```kotlin
 * val polygon = wgs84Polygon(
 *     126.97 to 37.56,
 *     126.99 to 37.56,
 *     126.99 to 37.57,
 *     126.97 to 37.57,
 *     126.97 to 37.56,
 * )
 * // polygon.numPoints == 5
 * ```
 *
 * @param points (lng, lat) 좌표 쌍 목록. 자동으로 닫힘 (첫 좌표 = 마지막 좌표).
 */
fun wgs84Polygon(vararg points: Pair<Double, Double>): Polygon {
    // JTS LinearRing은 최소 4좌표(3 고유 꼭짓점 + 닫힘 좌표)를 요구한다.
    // 닫힘 좌표는 아래에서 자동으로 추가되므로 입력값은 3개 이상이어야 유효한 Polygon이 된다.
    require(points.size >= 3) {
        "Polygon을 만들려면 최소 3개의 좌표가 필요합니다. 제공된 좌표 수: ${points.size}"
    }
    val coords = points.map { (lng, lat) -> Coordinate(lng, lat) }.toMutableList()
    if (coords.first().x != coords.last().x || coords.first().y != coords.last().y) {
        coords.add(coords.first())
    }
    return WGS84_FACTORY.createPolygon(coords.toTypedArray())
}

/**
 * WGS84 직사각형 Polygon을 생성한다.
 *
 * ```kotlin
 * val rect = wgs84Rectangle(126.97, 37.56, 126.99, 37.58)
 * // rect.numPoints == 5
 * // rect.isValid == true
 * ```
 *
 * @param minLng 최소 경도
 * @param minLat 최소 위도
 * @param maxLng 최대 경도
 * @param maxLat 최대 위도
 */
fun wgs84Rectangle(minLng: Double, minLat: Double, maxLng: Double, maxLat: Double): Polygon =
    wgs84Polygon(
        minLng to minLat,
        maxLng to minLat,
        maxLng to maxLat,
        minLng to maxLat,
        minLng to minLat,
    )

/**
 * WGS84 LineString을 생성한다.
 *
 * ```kotlin
 * val line = wgs84LineString(
 *     126.97 to 37.56,
 *     126.98 to 37.57,
 *     126.99 to 37.58,
 * )
 * // line.numPoints == 3
 * ```
 *
 * @param points (lng, lat) 좌표 쌍 목록
 */
fun wgs84LineString(vararg points: Pair<Double, Double>): LineString {
    // JTS LineString은 시작점과 끝점이 구분되어야 하므로 최소 2개의 좌표가 필요하다.
    // 1개 이하로 호출하면 createLineString()이 IllegalArgumentException을 던지는데,
    // 그보다 먼저 사용자 친화적 메시지로 조기에 실패하기 위해 명시적으로 검증한다.
    require(points.size >= 2) {
        "LineString을 만들려면 최소 2개의 좌표가 필요합니다. 제공된 좌표 수: ${points.size}"
    }
    val coords = points.map { (lng, lat) -> Coordinate(lng, lat) }.toTypedArray()
    return WGS84_FACTORY.createLineString(coords)
}

/**
 * WGS84 MultiPoint를 생성한다.
 *
 * ```kotlin
 * val mp = wgs84MultiPoint(
 *     wgs84Point(126.97, 37.56),
 *     wgs84Point(126.98, 37.57),
 * )
 * // mp.numGeometries == 2
 * ```
 *
 * @param points 포함할 [Point] 목록
 */
fun wgs84MultiPoint(vararg points: Point): MultiPoint =
    WGS84_FACTORY.createMultiPoint(points)

/**
 * WGS84 MultiPolygon을 생성한다.
 *
 * ```kotlin
 * val mp = wgs84MultiPolygon(
 *     wgs84Rectangle(126.97, 37.56, 126.98, 37.57),
 *     wgs84Rectangle(126.99, 37.58, 127.00, 37.59),
 * )
 * // mp.numGeometries == 2
 * ```
 *
 * @param polygons 포함할 [Polygon] 목록
 */
fun wgs84MultiPolygon(vararg polygons: Polygon): MultiPolygon =
    WGS84_FACTORY.createMultiPolygon(polygons)

/**
 * WGS84 MultiLineString을 생성한다.
 *
 * ```kotlin
 * val mls = wgs84MultiLineString(
 *     wgs84LineString(126.97 to 37.56, 126.98 to 37.57),
 *     wgs84LineString(126.99 to 37.58, 127.00 to 37.59),
 * )
 * // mls.numGeometries == 2
 * ```
 *
 * @param lineStrings 포함할 [LineString] 목록
 */
fun wgs84MultiLineString(vararg lineStrings: LineString): MultiLineString =
    WGS84_FACTORY.createMultiLineString(lineStrings)
