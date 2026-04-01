package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * WGS84 위경도 좌표를 나타내는 데이터 클래스입니다.
 *
 * @param latitude  위도 (-90.0 ~ 90.0)
 * @param longitude 경도 (-180.0 ~ 180.0)
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
): Comparable<GeoLocation>, Serializable {

    init {
        require(latitude in -90.0..90.0) { "위도는 -90~90 범위여야 합니다: $latitude" }
        require(longitude in -180.0..180.0) { "경도는 -180~180 범위여야 합니다: $longitude" }
    }

    companion object: KLogging() {
        private const val serialVersionUID = 1L

        /** 원점(0, 0) 좌표 */
        val ZERO = GeoLocation(0.0, 0.0)

        /** 서울 시청 좌표 */
        val SEOUL = GeoLocation(37.5665, 126.9780)

        /** 뉴욕 좌표 */
        val NEW_YORK = GeoLocation(40.7128, -74.0060)
    }

    /**
     * Haversine 공식으로 두 위치 사이의 거리(미터)를 계산합니다.
     *
     * @param other 대상 위치
     * @return 두 위치 사이의 거리 (미터)
     */
    fun distanceTo(other: GeoLocation): Double {
        val r = 6371000.0
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun compareTo(other: GeoLocation): Int {
        var diff = latitude.compareTo(other.latitude)
        if (diff == 0) diff = longitude.compareTo(other.longitude)
        return diff
    }
}
