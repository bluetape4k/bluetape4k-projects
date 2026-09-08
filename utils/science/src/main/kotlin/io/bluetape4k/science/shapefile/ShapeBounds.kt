package io.bluetape4k.science.shapefile

import io.bluetape4k.support.requireFinite
import java.io.Serializable

/**
 * Shapefile 원본 좌표계의 X/Y 경계를 나타냅니다.
 *
 * 위경도나 미터 단위를 추론하거나 좌표를 변환하지 않습니다. 생성자와 `copy`는
 * 유한한 값과 각 축의 최소/최대 순서만 검증합니다. 좌표계가 같은 도형에 사용해야 합니다.
 *
 * ```kotlin
 * val bounds = ShapeBounds(minX = 14000000.0, minY = 4000000.0, maxX = 15000000.0, maxY = 5000000.0)
 * val west = bounds.minX
 * ```
 *
 * @param minX X축 최솟값
 * @param minY Y축 최솟값
 * @param maxX X축 최댓값
 * @param maxY Y축 최댓값
 */
data class ShapeBounds(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
): Serializable {

    init {
        minX.requireFinite("minX")
        minY.requireFinite("minY")
        maxX.requireFinite("maxX")
        maxY.requireFinite("maxY")
        require(minX <= maxX) { "minX($minX)는 maxX($maxX)보다 작거나 같아야 합니다." }
        require(minY <= maxY) { "minY($minY)는 maxY($maxY)보다 작거나 같아야 합니다." }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
