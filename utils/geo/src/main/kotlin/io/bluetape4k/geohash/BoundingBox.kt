package io.bluetape4k.geohash

import io.bluetape4k.geohash.utils.remainderWithFix
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import java.io.Serializable

/**
 * 남서쪽 꼭지점 좌표와 북동쪽 꼭지점 좌표로 BoundingBox를 생성합니다.
 */
fun boundingBoxOf(
    southWestCorner: WGS84Point,
    northEastCorner: WGS84Point,
): BoundingBox = boundingBoxOf(
    southWestCorner.latitude,
    northEastCorner.latitude,
    southWestCorner.longitude,
    northEastCorner.longitude
)

/**
 * 남쪽 위도, 북쪽 위도, 서쪽 경도, 동쪽 경도로 BoundingBox를 생성합니다.
 */
fun boundingBoxOf(
    southLatitude: Double,
    northLatitude: Double,
    westLongitude: Double,
    eastLongitude: Double,
): BoundingBox = BoundingBox(southLatitude, northLatitude, westLongitude, eastLongitude)

/**
 * 4개의 좌표를 가지고 있는 BoundingBox입니다.
 *
 * 생성자와 `copy`는 좌표 범위를 검증합니다. 공개 setter는 호환성을 위해 유지하며,
 * 직접 변경한 좌표는 포함·교차·확장 연산과 직렬화 시 검증합니다.
 * 네 좌표를 동시에 변경하는 원자성이나 스레드 안전성은 보장하지 않습니다.
 *
 * @property southLatitude 남쪽 위도
 * @property northLatitude 북쪽 위도
 * @property westLongitude 서쪽 경도
 * @property eastLongitude 동쪽 경도
 */
data class BoundingBox(
    var southLatitude: Double,
    var northLatitude: Double,
    var westLongitude: Double,
    var eastLongitude: Double,
): Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = -1275515777007294183L
    }

    init {
        validateCoordinates()
    }

    private fun validateCoordinates() {
        southLatitude.requireInRange(-90.0, 90.0, "southLatitude")
        northLatitude.requireInRange(-90.0, 90.0, "northLatitude")
        westLongitude.requireInRange(-180.0, 180.0, "westLongitude")
        eastLongitude.requireInRange(-180.0, 180.0, "eastLongitude")
        require(southLatitude <= northLatitude) {
            "southLatitude[$southLatitude] must be less than or equal to northLatitude[$northLatitude]"
        }
    }

    private fun writeObject(output: java.io.ObjectOutputStream) {
        validateCoordinates()
        intersects180Meridian = eastLongitude < westLongitude
        output.defaultWriteObject()
    }

    private var intersects180Meridian: Boolean = eastLongitude < westLongitude

    /** 현재 경도로 자오선 교차 여부를 계산합니다. 직접 변경한 좌표도 조회 시 검증합니다. */
    val isIntersection180Meridian: Boolean
        get() {
            validateCoordinates()
            return eastLongitude < westLongitude
        }

    /**
     * 북동쪽 꼭지점 좌표를 반환합니다.
     */
    fun getNorthEastCorner(): WGS84Point = wgs84PointOf(northLatitude, eastLongitude)

    /**
     * 북서쪽 꼭지점 좌표를 반환합니다.
     */
    fun getNorthWestCorner(): WGS84Point = wgs84PointOf(northLatitude, westLongitude)

    /**
     * 남동쪽 꼭지점 좌표를 반환합니다.
     */
    fun getSouthEastCorner(): WGS84Point = wgs84PointOf(southLatitude, eastLongitude)

    /**
     * 남서쪽 꼭지점 좌표를 반환합니다.
     */
    fun getSouthWestCorner(): WGS84Point = wgs84PointOf(southLatitude, westLongitude)

    /**
     * BoundingBox의 위도 크기를 반환합니다.
     */
    fun getLatitudeSize(): Double {
        return northLatitude - southLatitude
    }

    /**
     * BoundingBox의 경도 크기를 반환합니다.
     */
    fun getLongitudeSize(): Double {
        if (eastLongitude == 180.0 && westLongitude == -180.0) return 360.0
        var size = (eastLongitude - westLongitude) % 360

        // Remainder fix for earlier java versions
        if (size < 0) size += 360.0

        return size
    }

    /**
     * BoundingBox가 주어진 좌표를 포함하는지 여부를 반환합니다.
     *
     * ```kotlin
     * val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * bbox.contains(wgs84PointOf(45.5, 120.5)).shouldBeTrue()
     * bbox.contains(wgs84PointOf(90.0, 90.0)).shouldBeFalse()
     * ```
     *
     * @param point 포함 여부를 확인할 좌표
     * @return 포함 여부
     */
    fun contains(point: WGS84Point): Boolean {
        validateCoordinates()
        return containsLatitude(point.latitude) && containsLongitude(point.longitude)
    }

    /**
     * BoundingBox가 주어진 BoundingBox를 포함하는지 여부를 반환합니다.
     *
     * ```kotlin
     * val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * val bbox1 = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * val bbox2 = boundingBoxOf(45.0, 46.0, 121.0, 122.0)
     * bbox.contains(bbox1).shouldBeTrue()
     * bbox.contains(bbox2).shouldBeFalse()
     * ```
     *
     * @param other 포함 여부를 확인할 BoundingBox
     * @return 포함 여부
     */
    fun intersects(other: BoundingBox): Boolean {
        validateCoordinates()
        other.validateCoordinates()
        // Check latitude first cause it's the same for all cases
        return if (other.southLatitude > northLatitude || other.northLatitude < southLatitude) {
            false
        } else {
            when {
                !isIntersection180Meridian && !other.isIntersection180Meridian ->
                    !(other.eastLongitude < westLongitude || other.westLongitude > eastLongitude)

                isIntersection180Meridian && !other.isIntersection180Meridian  ->
                    !(eastLongitude < other.westLongitude && westLongitude > other.eastLongitude)

                !isIntersection180Meridian && other.isIntersection180Meridian  ->
                    !(westLongitude > other.eastLongitude && eastLongitude < other.westLongitude)

                else                                                   -> true
            }
        }
    }

    /**
     * BoundingBox의 중심 좌표를 반환합니다.
     *
     * ```kotlin
     * val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * bbox.getCenter()  // wgs84PointOf(45.5, 120.5)
     * ```
     */
    fun getCenter(): WGS84Point {
        val centerLatitude = (southLatitude + northLatitude) / 2.0
        var centerLongitude = (eastLongitude + westLongitude) / 2.0
        if (centerLongitude > 180.0) {
            centerLongitude -= 360.0
        }
        return WGS84Point(centerLatitude, centerLongitude)
    }

    /**
     * BoundingBox가 주어진 좌표를 포함하도록 확장합니다.
     *
     * ```kotlin
     * val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * bbox.expandToInclude(wgs84PointOf(45.5, 120.5))  // boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * bbox.expandToInclude(wgs84PointOf(90.0, 90.0))  // boundingBoxOf(45.0, 90.0, 120.0, 121.0)
     * ```
     *
     * @param point 포함할 좌표
     */
    fun expandToInclude(point: WGS84Point) {
        validateCoordinates()
        // Expand Latitude
        if (point.latitude < southLatitude)
            southLatitude = point.latitude
        else if (point.latitude > northLatitude)
            northLatitude = point.latitude

        // Already done in this case
        if (containsLongitude(point.longitude)) return

        // If this is not the case compute the distance between the endpoints in east direction
        val distanceEastToPoint: Double = (point.longitude - eastLongitude).remainderWithFix(360)
        val distancePointToWest: Double = (westLongitude - point.longitude).remainderWithFix(360)

        // The minimal distance needs to be extended

        // The minimal distance needs to be extended
        if (distanceEastToPoint <= distancePointToWest)
            eastLongitude = point.longitude
        else
            westLongitude = point.longitude

        intersects180Meridian = eastLongitude < westLongitude
    }

    /**
     * BoundingBox가 주어진 BoundingBox를 포함하도록 확장합니다.
     *
     * ```kotlin
     * val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
     * val bbox1 = boundingBoxOf(45.5, 46.5, 120.5, 121.5)
     * bbox.expandToInclude(bbox1)  // boundingBoxOf(45.0, 46.5, 120.0, 121.5)
     * ```
     *
     * @param other 포함할 BoundingBox
     */
    fun expandToInclude(other: BoundingBox) {
        validateCoordinates()
        other.validateCoordinates()
        // Expand Latitude
        if (other.southLatitude < southLatitude) {
            southLatitude = other.southLatitude
        } else if (other.northLatitude > northLatitude) {
            northLatitude = other.northLatitude
        }

        // Expand Longitude
        // At first check whether the two boxes contain each other or not
        val thisContainsOther = containsLongitude(other.eastLongitude) && containsLongitude(other.westLongitude)
        val otherContainsThis = other.containsLongitude(eastLongitude) && other.containsLongitude(westLongitude)

        // The new box needs to span the whole globe
        if (thisContainsOther && otherContainsThis) {
            eastLongitude = 180.0
            westLongitude = -180.0
            intersects180Meridian = false
            return
        }
        // Already done in this case
        if (thisContainsOther) return

        // Expand to match the bigger box
        if (otherContainsThis) {
            eastLongitude = other.eastLongitude
            westLongitude = other.westLongitude
            intersects180Meridian = eastLongitude < westLongitude
            return
        }

        // If this is not the case compute the distance between the endpoints in east direction
        val distanceEastToOtherEast: Double = (other.eastLongitude - eastLongitude).remainderWithFix(360)
        val distanceOtherWestToWest: Double = (westLongitude - other.westLongitude).remainderWithFix(360)

        // The minimal distance needs to be extended
        if (distanceEastToOtherEast <= distanceOtherWestToWest) {
            eastLongitude = other.eastLongitude
        } else {
            westLongitude = other.westLongitude
        }

        intersects180Meridian = eastLongitude < westLongitude
    }

    private fun containsLatitude(latitude: Double): Boolean {
        return latitude in southLatitude..northLatitude
    }

    private fun containsLongitude(longitude: Double): Boolean {
        return if (isIntersection180Meridian) {
            longitude >= westLongitude || longitude <= eastLongitude
        } else {
            longitude in westLongitude..eastLongitude
        }
    }
}
