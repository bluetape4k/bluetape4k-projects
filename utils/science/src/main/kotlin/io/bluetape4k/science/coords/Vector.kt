package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 방향(도)과 거리(미터)로 구성된 벡터를 나타내는 클래스입니다.
 *
 * 각도는 +X 축(동쪽)으로부터 시계 반대 방향으로 증가합니다.
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
