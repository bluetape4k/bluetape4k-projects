package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 방향(도)과 거리(미터)로 구성된 벡터를 나타내는 클래스입니다.
 *
 * 각도는 +X 축(동쪽)으로부터 시계 반대 방향으로 증가합니다.
 *
 * ```kotlin
 * val north = Vector(degree = 90.0, distance = 1000.0)
 * println(north.degree)   // 90.0 (북쪽)
 * println(north.distance) // 1000.0 (1km)
 *
 * val east = Vector(degree = 0.0, distance = 500.0)
 * println(east.degree)    // 0.0 (동쪽)
 * ```
 *
 * @param degree   방향 각도 (0~360도)
 * @param distance 거리 (미터)
 */
data class Vector(
    val degree: Double,
    val distance: Double,
): Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}
