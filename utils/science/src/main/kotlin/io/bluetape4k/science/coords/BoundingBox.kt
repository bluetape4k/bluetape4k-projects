package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.locationtech.jts.geom.Envelope
import java.io.Serializable

/**
 * 위경도 좌표로 정의된 직사각형 경계 영역(Bounding Box)을 나타내는 클래스입니다.
 *
 * ```kotlin
 * val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
 * println(korea.width)  // 7.0
 * println(korea.height) // 5.9
 * ```
 *
 * @param minLat 최소 위도 (남쪽 경계)
 * @param minLon 최소 경도 (서쪽 경계)
 * @param maxLat 최대 위도 (북쪽 경계)
 * @param maxLon 최대 경도 (동쪽 경계)
 */
data class BoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
): Comparable<BoundingBox>, Serializable {

    init {
        require(minLat <= maxLat) { "minLat($minLat)는 maxLat($maxLat)보다 작거나 같아야 합니다." }
        require(minLon <= maxLon) { "minLon($minLon)는 maxLon($maxLon)보다 작거나 같아야 합니다." }
    }

    companion object: KLogging() {
        private const val serialVersionUID = 1L

        /**
         * 두 [GeoLocation]으로부터 [BoundingBox]를 생성합니다.
         *
         * ```kotlin
         * val seoul = GeoLocation(37.5665, 126.9780)
         * val busan = GeoLocation(35.1796, 129.0756)
         * val bbox = BoundingBox.of(seoul, busan)
         * println(bbox.minLat) // 35.1796
         * println(bbox.maxLon) // 129.0756
         * ```
         *
         * @param loc1 첫 번째 위치
         * @param loc2 두 번째 위치
         */
        fun of(loc1: GeoLocation, loc2: GeoLocation): BoundingBox = BoundingBox(
            minLat = minOf(loc1.latitude, loc2.latitude),
            minLon = minOf(loc1.longitude, loc2.longitude),
            maxLat = maxOf(loc1.latitude, loc2.latitude),
            maxLon = maxOf(loc1.longitude, loc2.longitude),
        )
    }

    /**
     * 경계 영역의 너비 (경도 차이)
     *
     * ```kotlin
     * val bbox = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
     * println(bbox.width) // 7.0
     * ```
     */
    val width: Double get() = maxLon - minLon

    /**
     * 경계 영역의 높이 (위도 차이)
     *
     * ```kotlin
     * val bbox = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
     * println(bbox.height) // 5.9
     * ```
     */
    val height: Double get() = maxLat - minLat

    /**
     * 주어진 [GeoLocation]이 이 경계 영역 안에 포함되는지 여부를 반환합니다.
     *
     * ```kotlin
     * val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
     * val seoul = GeoLocation(37.5665, 126.9780)
     * println(korea.contains(seoul)) // true
     * val newYork = GeoLocation(40.7128, -74.0060)
     * println(korea.contains(newYork)) // false
     * ```
     *
     * @param location 확인할 위치
     */
    fun contains(location: GeoLocation): Boolean =
        location.latitude in minLat..maxLat && location.longitude in minLon..maxLon

    /**
     * 주어진 [BoundingBox]가 이 경계 영역 안에 완전히 포함되는지 여부를 반환합니다.
     *
     * ```kotlin
     * val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
     * val seoulArea = BoundingBox(minLat = 37.4, minLon = 126.7, maxLat = 37.7, maxLon = 127.2)
     * println(korea.contains(seoulArea)) // true
     * ```
     *
     * @param other 확인할 경계 영역
     */
    fun contains(other: BoundingBox): Boolean =
        other.minLat >= minLat && other.maxLat <= maxLat &&
        other.minLon >= minLon && other.maxLon <= maxLon

    /**
     * 주어진 [BoundingBox]와 이 경계 영역이 겹치는지 여부를 반환합니다.
     *
     * ```kotlin
     * val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
     * val seoulArea = BoundingBox(minLat = 37.4, minLon = 126.7, maxLat = 37.7, maxLon = 127.2)
     * println(korea.intersects(seoulArea)) // true
     * val usa = BoundingBox(minLat = 24.0, minLon = -125.0, maxLat = 49.0, maxLon = -66.0)
     * println(korea.intersects(usa)) // false
     * ```
     *
     * @param other 확인할 경계 영역
     */
    fun intersects(other: BoundingBox): Boolean =
        minLat <= other.maxLat && maxLat >= other.minLat &&
        minLon <= other.maxLon && maxLon >= other.minLon

    /**
     * 이 경계 영역과 주어진 [BoundingBox]를 모두 포함하는 최소 경계 영역을 반환합니다.
     *
     * ```kotlin
     * val bbox1 = BoundingBox(minLat = 0.0, minLon = 0.0, maxLat = 10.0, maxLon = 10.0)
     * val bbox2 = BoundingBox(minLat = 5.0, minLon = 5.0, maxLat = 20.0, maxLon = 20.0)
     * val union = bbox1.union(bbox2)
     * println(union.minLat) // 0.0
     * println(union.maxLat) // 20.0
     * println(union.maxLon) // 20.0
     * ```
     *
     * @param other 합칠 경계 영역
     */
    fun union(other: BoundingBox): BoundingBox = BoundingBox(
        minLat = minOf(minLat, other.minLat),
        minLon = minOf(minLon, other.minLon),
        maxLat = maxOf(maxLat, other.maxLat),
        maxLon = maxOf(maxLon, other.maxLon),
    )

    /**
     * 경계 영역의 중심 좌표를 반환합니다.
     *
     * ```kotlin
     * val bbox = BoundingBox(minLat = 0.0, minLon = 0.0, maxLat = 10.0, maxLon = 20.0)
     * val center = bbox.center()
     * println(center.latitude)  // 5.0
     * println(center.longitude) // 10.0
     * ```
     */
    fun center(): GeoLocation = GeoLocation(
        latitude = (minLat + maxLat) / 2.0,
        longitude = (minLon + maxLon) / 2.0,
    )

    /**
     * 이 [BoundingBox]와 주어진 [BoundingBox] 간의 공간 관계([BoundingBoxRelation])를 반환합니다.
     *
     * ```kotlin
     * val korea = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
     * val seoulArea = BoundingBox(minLat = 37.4, minLon = 126.7, maxLat = 37.7, maxLon = 127.2)
     * println(korea.relationTo(seoulArea))   // CONTAINS
     * println(seoulArea.relationTo(korea))   // WITHIN
     * val usa = BoundingBox(minLat = 24.0, minLon = -125.0, maxLat = 49.0, maxLon = -66.0)
     * println(korea.relationTo(usa))         // DISJOINT
     * ```
     *
     * @param other 비교할 경계 영역
     */
    fun relationTo(other: BoundingBox): BoundingBoxRelation = when {
        contains(other) -> BoundingBoxRelation.CONTAINS
        other.contains(this) -> BoundingBoxRelation.WITHIN
        intersects(other) -> BoundingBoxRelation.INTERSECTS
        else -> BoundingBoxRelation.DISJOINT
    }

    override fun compareTo(other: BoundingBox): Int {
        var diff = minLat.compareTo(other.minLat)
        if (diff == 0) diff = minLon.compareTo(other.minLon)
        if (diff == 0) diff = maxLat.compareTo(other.maxLat)
        if (diff == 0) diff = maxLon.compareTo(other.maxLon)
        return diff
    }
}

/**
 * [BoundingBox]를 JTS [Envelope]으로 변환합니다.
 *
 * ```kotlin
 * val bbox = BoundingBox(minLat = 33.0, minLon = 124.0, maxLat = 38.9, maxLon = 131.0)
 * val env = bbox.toEnvelope()
 * println(env.minX) // 124.0
 * println(env.maxX) // 131.0
 * println(env.minY) // 33.0
 * println(env.maxY) // 38.9
 * ```
 */
fun BoundingBox.toEnvelope(): Envelope =
    Envelope(minLon, maxLon, minLat, maxLat)
