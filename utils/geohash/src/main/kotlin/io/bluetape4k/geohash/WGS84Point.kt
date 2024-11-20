package io.bluetape4k.geohash

import io.bluetape4k.geohash.utils.VincentyGeodesy
import io.bluetape4k.support.requireInRange
import java.io.Serializable

/**
 * WGS84 좌표계의 위도와 경도를 나타냅니다.
 *
 * @property latitude 위도
 * @property longitude 경도
 */
data class WGS84Point(
    val latitude: Double,
    val longitude: Double,
): Serializable {
    init {
        latitude.requireInRange(-90.0, 90.0, "latitude")
        longitude.requireInRange(-180.0, 180.0, "longitude")
    }

    fun toPair(): Pair<Double, Double> = latitude to longitude
}

/**
 * 위도와 경도로 WGS84 좌표를 생성합니다.
 *
 * @param latitude 위도
 * @param longitude 경도
 * @return WGS84Point
 */
fun wgs84PointOf(latitude: Double, longitude: Double): WGS84Point {
    latitude.requireInRange(-90.0, 90.0, "latitude")
    longitude.requireInRange(-180.0, 180.0, "longitude")
    return WGS84Point(latitude, longitude)
}

/**
 * WGS84 좌표를 이동합니다.
 *
 * @param bearingInDegrees 이동 방향 각도
 * @param distanceInMeters 이동 거리 (미터)
 * @return WGS84Point
 */
fun WGS84Point.moveInDirection(
    bearingInDegrees: Double,
    distanceInMeters: Double,
): WGS84Point {
    return VincentyGeodesy.moveInDirection(this, bearingInDegrees, distanceInMeters)
}

/**
 * 두 WGS84 좌표 사이의 거리를 계산합니다.
 *
 * @param other 다른 WGS84 좌표
 * @return Double
 */
fun WGS84Point.distanceInMeters(other: WGS84Point): Double {
    return VincentyGeodesy.distanceInMeters(this, other)
}
