package io.bluetape4k.science.geometry

import io.bluetape4k.science.coords.BoundingBox
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * [Polygon]의 면적을 제곱미터(m²) 단위로 반환합니다.
 *
 * 주의: 이 계산은 평면 직교 좌표계(투영 좌표계) 기준입니다.
 * WGS84 위경도 좌표계에서는 실제 면적과 차이가 발생합니다.
 */
fun Polygon.areaInSquareMeters(): Double = area

/**
 * [Polygon]의 무게 중심(centroid)을 JTS [Point]로 반환합니다.
 */
fun Polygon.centroid(): Point = centroid

/**
 * [Polygon]의 외접 사각형([BoundingBox])을 반환합니다.
 *
 * 폴리곤의 envelope에서 최소/최대 위경도를 추출합니다.
 */
fun Polygon.toBoundingBox(): BoundingBox {
    val env = envelopeInternal
    return BoundingBox(
        minLat = env.minY,
        minLon = env.minX,
        maxLat = env.maxY,
        maxLon = env.maxX,
    )
}
