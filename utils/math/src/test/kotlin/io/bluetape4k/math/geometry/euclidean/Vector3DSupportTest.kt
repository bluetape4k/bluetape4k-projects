package io.bluetape4k.math.geometry.euclidean

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.junit.jupiter.api.Test
import kotlin.math.PI

class Vector3DSupportTest {

    companion object : KLogging()

    @Test
    fun `DoubleArray를 3차원 벡터로 변환할 수 있다`() {
        val v = doubleArrayOf(1.0, 2.0, 3.0).toVector3D()
        v.x.shouldBeNear(1.0, 1e-10)
        v.y.shouldBeNear(2.0, 1e-10)
        v.z.shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `x, y, z 좌표로 3차원 벡터를 생성할 수 있다`() {
        val v = vector3DOf(1.0, 2.0, 3.0)
        v.x.shouldBeNear(1.0, 1e-10)
        v.y.shouldBeNear(2.0, 1e-10)
        v.z.shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `구면 좌표로 3차원 벡터를 생성할 수 있다`() {
        val v = vector3DOf(alpha = 0.0, delta = 0.0)
        v.x.shouldBeNear(1.0, 1e-10)
        v.y.shouldBeNear(0.0, 1e-10)
        v.z.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `스칼라와 벡터의 선형 결합으로 3차원 벡터를 생성한다`() {
        val u = Vector3D.PLUS_I
        val v = vector3DOf(2.0, u)
        v.x.shouldBeNear(2.0, 1e-10)
        v.y.shouldBeNear(0.0, 1e-10)
        v.z.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `두 스칼라-벡터 쌍의 선형 결합으로 3차원 벡터를 생성한다`() {
        val u1 = Vector3D.PLUS_I
        val u2 = Vector3D.PLUS_J
        val v = vector3DOf(2.0, u1, 3.0, u2)
        v.x.shouldBeNear(2.0, 1e-10)
        v.y.shouldBeNear(3.0, 1e-10)
        v.z.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `세 스칼라-벡터 쌍의 선형 결합으로 3차원 벡터를 생성한다`() {
        val v = vector3DOf(1.0, Vector3D.PLUS_I, 2.0, Vector3D.PLUS_J, 3.0, Vector3D.PLUS_K)
        v.x.shouldBeNear(1.0, 1e-10)
        v.y.shouldBeNear(2.0, 1e-10)
        v.z.shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `네 스칼라-벡터 쌍의 선형 결합으로 3차원 벡터를 생성한다`() {
        val u = Vector3D.PLUS_I
        val v = vector3DOf(1.0, u, 2.0, u, 3.0, u, 4.0, u)
        v.x.shouldBeNear(10.0, 1e-10)
        v.y.shouldBeNear(0.0, 1e-10)
        v.z.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `두 3차원 벡터를 더할 수 있다`() {
        val v1 = vector3DOf(1.0, 2.0, 3.0)
        val v2 = vector3DOf(4.0, 5.0, 6.0)
        val result = v1 + v2
        result.x.shouldBeNear(5.0, 1e-10)
        result.y.shouldBeNear(7.0, 1e-10)
        result.z.shouldBeNear(9.0, 1e-10)
    }

    @Test
    fun `두 3차원 벡터를 뺄 수 있다`() {
        val v1 = vector3DOf(5.0, 7.0, 9.0)
        val v2 = vector3DOf(1.0, 2.0, 3.0)
        val result = v1 - v2
        result.x.shouldBeNear(4.0, 1e-10)
        result.y.shouldBeNear(5.0, 1e-10)
        result.z.shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `두 3차원 벡터 사이의 각도를 계산할 수 있다`() {
        val v1 = vector3DOf(1.0, 0.0, 0.0)
        val v2 = vector3DOf(0.0, 1.0, 0.0)
        val angle = v1.angle(v2)
        angle.shouldBeNear(PI / 2, 1e-10)
    }
}
