package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class VectorTest {

    companion object: KLogging()

    @Test
    fun `Vector 생성 시 degree와 distance가 올바르게 저장된다`() {
        val vector = Vector(degree = 90.0, distance = 1000.0)
        vector.degree shouldBeEqualTo 90.0
        vector.distance shouldBeEqualTo 1000.0
    }

    @Test
    fun `동일한 속성의 Vector는 동등하다`() {
        val v1 = Vector(degree = 45.0, distance = 500.0)
        val v2 = Vector(degree = 45.0, distance = 500.0)
        (v1 == v2).shouldBeTrue()
        (v1.hashCode() == v2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `다른 속성의 Vector는 동등하지 않다`() {
        val v1 = Vector(degree = 45.0, distance = 500.0)
        val v2 = Vector(degree = 90.0, distance = 500.0)
        (v1 == v2).shouldBeFalse()
    }

    @Test
    fun `Vector copy가 정상 동작한다`() {
        val original = Vector(degree = 0.0, distance = 100.0)
        val copied = original.copy(degree = 180.0)
        copied.degree shouldBeEqualTo 180.0
        copied.distance shouldBeEqualTo 100.0
        original.degree shouldBeEqualTo 0.0
    }

    @Test
    fun `0도 벡터는 동쪽을 나타낸다`() {
        val east = Vector(degree = 0.0, distance = 1000.0)
        east.degree shouldBeEqualTo 0.0
    }

    @Test
    fun `360도에 가까운 방향을 나타낼 수 있다`() {
        val almostNorth = Vector(degree = 359.9, distance = 200.0)
        almostNorth.degree shouldBeEqualTo 359.9
        almostNorth.distance shouldBeEqualTo 200.0
    }
}
