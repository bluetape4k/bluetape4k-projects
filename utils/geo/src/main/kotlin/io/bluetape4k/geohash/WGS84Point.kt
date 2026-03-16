package io.bluetape4k.geohash

import io.bluetape4k.geohash.utils.VincentyGeodesy
import io.bluetape4k.support.requireInRange
import java.io.Serializable

/**
 * WGS84 좌표계의 위도와 경도를 나타냅니다.
 *
 * ## 동작/계약
 * - 생성 시 `latitude`는 `[-90, 90]`, `longitude`는 `[-180, 180]` 범위를 검증합니다.
 * - 범위를 벗어나면 `requireInRange` 검증에 의해 예외가 발생합니다.
 * - 불변 `data class`로서 좌표 연산은 새 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val point = WGS84Point(37.5665, 126.9780)
 * val pair = point.toPair()
 * // pair == (37.5665 to 126.9780)
 * ```
 */
data class WGS84Point(
    val latitude: Double,
    val longitude: Double,
): Serializable {
    init {
        latitude.requireInRange(-90.0, 90.0, "latitude")
        longitude.requireInRange(-180.0, 180.0, "longitude")
    }

    /**
     * 좌표를 `(위도, 경도)` 쌍으로 반환합니다.
     *
     * ## 동작/계약
     * - 새 [Pair]를 생성해 반환합니다.
     * - 원본 좌표 값은 변경되지 않습니다.
     *
     * ```kotlin
     * val pair = WGS84Point(10.0, 20.0).toPair()
     * // pair == (10.0 to 20.0)
     * ```
     */
    fun toPair(): Pair<Double, Double> = latitude to longitude
}

/**
 * 위도와 경도로 WGS84 좌표를 생성합니다.
 *
 * ## 동작/계약
 * - `latitude`는 `[-90, 90]`, `longitude`는 `[-180, 180]` 범위를 검증합니다.
 * - 범위 위반 시 예외가 발생합니다.
 *
 * @param latitude 위도
 * @param longitude 경도
 *
 * ```kotlin
 * val point = wgs84PointOf(37.5665, 126.9780)
 * // point.latitude == 37.5665
 * ```
 */
fun wgs84PointOf(latitude: Double, longitude: Double): WGS84Point {
    latitude.requireInRange(-90.0, 90.0, "latitude")
    longitude.requireInRange(-180.0, 180.0, "longitude")
    return WGS84Point(latitude, longitude)
}

/**
 * 현재 좌표를 지정한 방위각과 거리만큼 이동한 새 좌표를 계산합니다.
 *
 * ## 동작/계약
 * - Vincenty 알고리즘을 사용하며 수신 객체는 변경하지 않습니다.
 * - `bearingInDegrees` 범위 검증은 내부 구현에서 수행됩니다.
 *
 * ```kotlin
 * val start = wgs84PointOf(37.5665, 126.9780)
 * val moved = start.moveInDirection(90.0, 1000.0)
 * // moved != start
 * ```
 */
fun WGS84Point.moveInDirection(
    bearingInDegrees: Double,
    distanceInMeters: Double,
): WGS84Point {
    return VincentyGeodesy.moveInDirection(this, bearingInDegrees, distanceInMeters)
}

/**
 * 두 좌표 사이의 Vincenty 거리(미터)를 계산합니다.
 *
 * ## 동작/계약
 * - 수신 객체와 [other]를 변경하지 않습니다.
 * - 계산 수렴 실패 시 내부 구현에서 `Double.NaN`이 반환될 수 있습니다.
 *
 * ```kotlin
 * val a = wgs84PointOf(37.5665, 126.9780)
 * val b = wgs84PointOf(37.5651, 126.9895)
 * val distance = a.distanceInMeters(b)
 * // distance > 0.0
 * ```
 */
fun WGS84Point.distanceInMeters(other: WGS84Point): Double {
    return VincentyGeodesy.distanceInMeters(this, other)
}
