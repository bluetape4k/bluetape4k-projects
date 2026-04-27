package io.bluetape4k.math.geometry.euclidean

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import org.junit.jupiter.api.Test
import kotlin.math.PI

class Vector2DSupportTest {

    companion object : KLogging()

    @Test
    fun `DoubleArray를 2차원 벡터로 변환할 수 있다`() {
        val v = doubleArrayOf(3.0, 4.0).toVector2D()
        v.x.shouldBeNear(3.0, 1e-10)
        v.y.shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `x, y 좌표로 2차원 벡터를 생성할 수 있다`() {
        val v = vector2DOf(3.0, 4.0)
        v.x.shouldBeNear(3.0, 1e-10)
        v.y.shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `두 2차원 벡터를 더할 수 있다`() {
        val v1 = vector2DOf(1.0, 2.0)
        val v2 = vector2DOf(3.0, 4.0)
        val result = v1 + v2
        result.x.shouldBeNear(4.0, 1e-10)
        result.y.shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `두 2차원 벡터를 뺄 수 있다`() {
        val v1 = vector2DOf(5.0, 7.0)
        val v2 = vector2DOf(2.0, 3.0)
        val result = v1 - v2
        result.x.shouldBeNear(3.0, 1e-10)
        result.y.shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `스칼라와 2차원 벡터를 곱할 수 있다`() {
        val v = vector2DOf(1.0, 2.0)
        val result = 3.0 * v
        result.x.shouldBeNear(3.0, 1e-10)
        result.y.shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `두 2차원 벡터 사이의 각도를 계산할 수 있다`() {
        val v1 = vector2DOf(1.0, 0.0)
        val v2 = vector2DOf(0.0, 1.0)
        val angle = v1.angle(v2)
        angle.shouldBeNear(PI / 2, 1e-10)
    }

    @Test
    fun `동일한 방향의 두 벡터의 각도는 0이다`() {
        val v1 = vector2DOf(1.0, 0.0)
        val v2 = vector2DOf(2.0, 0.0)
        val angle = v1.angle(v2)
        angle.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `영벡터가 아닌 벡터의 노름이 올바르다`() {
        val v = vector2DOf(3.0, 4.0)
        v.norm.shouldBeNear(5.0, 1e-10)
    }
}
