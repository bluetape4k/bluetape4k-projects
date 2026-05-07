package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

@RandomizedTest
class MeasureTest {

    companion object: KLogging()

    private class CoreUnit(
        suffix: String,
        ratio: Double = 1.0,
    ): Units(suffix, ratio)

    @Test
    fun `toNearest가 동작한다`() {
        val unit = CoreUnit("u")
        val rounded = (10.26 * unit).toNearest(0.1)
        (rounded `in` unit).shouldBeNear(10.3, 1e-10)
    }

    @Test
    fun `toNearest에 0 이하 값은 예외를 발생시킨다`() {
        val unit = CoreUnit("u")
        val measure = 10.0 * unit
        assertFailsWith<IllegalArgumentException> {
            measure.toNearest(0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            measure.toNearest(-1.0)
        }
    }

    @Test
    fun `문자열 표현이 단위를 포함한다`() {
        val unit = CoreUnit("u")
        val value = 42 * unit
        value.toString() shouldBeEqualTo "42.0 u"
    }

    @Test
    fun `동일 단위 동일 값의 equals가 동작한다`() {
        val unit = CoreUnit("u")
        val a = 10.0 * unit
        val b = 10.0 * unit
        (a == b).shouldBeTrue()
        (a.hashCode() == b.hashCode()).shouldBeTrue()
    }

    @Test
    fun `다른 ratio 단위간 equals가 amount 기반으로 비교한다`() {
        val small = CoreUnit("s", 1.0)
        val big = CoreUnit("b", 1000.0)

        val a = 1000.0 * small
        val b = 1.0 * big
        (a == b).shouldBeTrue()
    }

    @Test
    fun `다른 값의 equals는 false`() {
        val unit = CoreUnit("u")
        val a = 10.0 * unit
        val b = 20.0 * unit
        (a == b).shouldBeFalse()
    }

    @Test
    fun `unaryMinus가 부호를 반전한다`() {
        val unit = CoreUnit("u")
        val positive = 5.0 * unit
        val negative = -positive
        (negative `in` unit).shouldBeNear(-5.0, 1e-10)
    }

    @Test
    fun `스칼라 곱셈과 나눗셈이 동작한다`() {
        val unit = CoreUnit("u")
        val base = 10.0 * unit

        val doubled = base * 2
        (doubled `in` unit).shouldBeNear(20.0, 1e-10)

        val halved = base / 2
        (halved `in` unit).shouldBeNear(5.0, 1e-10)
    }
}
