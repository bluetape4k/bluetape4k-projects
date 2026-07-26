package io.bluetape4k.math.geometry.euclidean

import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.logging.KLogging
import org.apache.commons.math3.geometry.euclidean.oned.Vector1D
import org.junit.jupiter.api.Test

class Vector1DSupportTest {

    companion object: KLogging()

    @Test
    fun `두 1차원 벡터를 더할 수 있다`() {
        val v1 = Vector1D(1.0)
        val v2 = Vector1D(2.0)
        val result = v1 + v2
        result.x.shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `두 1차원 벡터를 뺄 수 있다`() {
        val v1 = Vector1D(5.0)
        val v2 = Vector1D(3.0)
        val result = v1 - v2
        result.x.shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `숫자를 1차원 벡터로 변환할 수 있다`() {
        val v = 3.14.toVector1D()
        v.x.shouldBeNear(3.14, 1e-10)
    }

    @Test
    fun `Int를 1차원 벡터로 변환할 수 있다`() {
        val v = 5.toVector1D()
        v.x.shouldBeNear(5.0, 1e-10)
    }

    @Test
    fun `스칼라와 벡터의 선형 결합으로 1차원 벡터를 생성한다`() {
        val u = Vector1D(2.0)
        val v = vector1DOf(3.0, u)
        v.x.shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `두 스칼라-벡터 쌍의 선형 결합으로 1차원 벡터를 생성한다`() {
        val u1 = Vector1D(1.0)
        val u2 = Vector1D(2.0)
        val v = vector1DOf(2.0, u1, 3.0, u2)
        v.x.shouldBeNear(8.0, 1e-10)
    }

    @Test
    fun `세 스칼라-벡터 쌍의 선형 결합으로 1차원 벡터를 생성한다`() {
        val u1 = Vector1D(1.0)
        val u2 = Vector1D(2.0)
        val u3 = Vector1D(3.0)
        val v = vector1DOf(1.0, u1, 2.0, u2, 3.0, u3)
        v.x.shouldBeNear(14.0, 1e-10)
    }

    @Test
    fun `네 스칼라-벡터 쌍의 선형 결합으로 1차원 벡터를 생성한다`() {
        val u = Vector1D(1.0)
        val v = vector1DOf(1.0, u, 2.0, u, 3.0, u, 4.0, u)
        v.x.shouldBeNear(10.0, 1e-10)
    }
}
