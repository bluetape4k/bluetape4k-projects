package io.bluetape4k.science.shapefile

import io.bluetape4k.science.coords.BoundingBox
import io.bluetape4k.science.coords.GeoLocation
import org.locationtech.jts.geom.Point

/**
 * [Shape]에서 Point 유형의 도형만 추출하여 [GeoLocation] 목록으로 반환합니다.
 *
 * Point 타입이 아닌 도형은 무시합니다.
 *
 * @return Point 도형의 [GeoLocation] 목록
 */
fun Shape.toGeoLocations(): List<GeoLocation> =
    records.mapNotNull { record ->
        val geom = record.geometry
        if (geom is Point) {
            val lat = geom.y
            val lon = geom.x
            if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                GeoLocation(lat, lon)
            } else null
        } else null
    }

/**
 * 주어진 [BoundingBox] 안에 포함되는 레코드만 필터링한 [Shape]를 반환합니다.
 *
 * 레코드의 도형 중심점(centroid)이 [bbox] 내에 있는 경우만 포함합니다.
 * bbox가 null인 레코드는 제외됩니다.
 *
 * @param bbox 필터링할 경계 사각형
 * @return 필터링된 [Shape]
 */
fun Shape.filterByBoundingBox(bbox: BoundingBox): Shape {
    val filtered = records.filter { record ->
        val centroid = record.geometry.centroid
        val lat = centroid.y
        val lon = centroid.x
        lat in bbox.minLat..bbox.maxLat && lon in bbox.minLon..bbox.maxLon
    }
    return copy(records = filtered)
}

/**
 * 레코드의 특정 속성 값으로 필터링한 [Shape]를 반환합니다.
 *
 * @param attributeName 속성 이름
 * @param predicate     속성 값 필터 조건
 * @return 필터링된 [Shape]
 */
fun Shape.filterByAttribute(attributeName: String, predicate: (Any?) -> Boolean): Shape {
    val filtered = records.filter { record ->
        predicate(record.attributes[attributeName])
    }
    return copy(records = filtered)
}

/**
 * 레코드 목록에서 특정 속성의 고유 값 집합을 반환합니다.
 *
 * @param attributeName 속성 이름
 * @return 고유 속성 값 집합
 */
fun Shape.distinctAttributeValues(attributeName: String): Set<Any?> =
    records.mapTo(LinkedHashSet()) { it.attributes[attributeName] }

/**
 * 전체 레코드를 포괄하는 [BoundingBox]를 계산합니다.
 *
 * 레코드가 없으면 null을 반환합니다.
 *
 * @return 전체 경계 사각형 또는 null
 */
fun Shape.computeBoundingBox(): BoundingBox? {
    val bboxes = records.mapNotNull { it.bbox }
    if (bboxes.isEmpty()) return null
    return bboxes.reduce { acc, b ->
        BoundingBox(
            minLat = minOf(acc.minLat, b.minLat),
            minLon = minOf(acc.minLon, b.minLon),
            maxLat = maxOf(acc.maxLat, b.maxLat),
            maxLon = maxOf(acc.maxLon, b.maxLon),
        )
    }
}
