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
 */
val WGS84_FACTORY: GeometryFactory = GeometryFactory(PrecisionModel(), SRID_WGS84)

/**
 * WGS84 Point를 생성한다.
 *
 * @param lng 경도 (X축, -180 ~ 180)
 * @param lat 위도 (Y축, -90 ~ 90)
 */
fun wgs84Point(lng: Double, lat: Double): Point =
    WGS84_FACTORY.createPoint(Coordinate(lng, lat))

/**
 * WGS84 Polygon을 생성한다.
 *
 * @param points (lng, lat) 좌표 쌍 목록. 자동으로 닫힘 (첫 좌표 = 마지막 좌표).
 */
fun wgs84Polygon(vararg points: Pair<Double, Double>): Polygon {
    val coords = points.map { (lng, lat) -> Coordinate(lng, lat) }.toMutableList()
    if (coords.first().x != coords.last().x || coords.first().y != coords.last().y) {
        coords.add(coords.first())
    }
    return WGS84_FACTORY.createPolygon(coords.toTypedArray())
}

/**
 * WGS84 직사각형 Polygon을 생성한다.
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
 * @param points (lng, lat) 좌표 쌍 목록
 */
fun wgs84LineString(vararg points: Pair<Double, Double>): LineString {
    val coords = points.map { (lng, lat) -> Coordinate(lng, lat) }.toTypedArray()
    return WGS84_FACTORY.createLineString(coords)
}

/**
 * WGS84 MultiPoint를 생성한다.
 */
fun wgs84MultiPoint(vararg points: Point): MultiPoint =
    WGS84_FACTORY.createMultiPoint(points)

/**
 * WGS84 MultiPolygon을 생성한다.
 */
fun wgs84MultiPolygon(vararg polygons: Polygon): MultiPolygon =
    WGS84_FACTORY.createMultiPolygon(polygons)

/**
 * WGS84 MultiLineString을 생성한다.
 */
fun wgs84MultiLineString(vararg lineStrings: LineString): MultiLineString =
    WGS84_FACTORY.createMultiLineString(lineStrings)
