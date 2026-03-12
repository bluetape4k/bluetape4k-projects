package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class GraphicsLengthTest {

    companion object: KLogging()

    @Test
    fun `픽셀 단위가 동작한다`() {
        (10.pixels() `in` GraphicsLength.pixels) shouldBeEqualTo 10.0
    }

    @Test
    fun `픽셀 toHuman 이 표시된다`() {
        10.pixels().toHuman() shouldBeEqualTo "10.0 px"
    }

    // ----- 사칙 연산 -----

    @Test
    fun `픽셀 사칙연산이 동작한다`() {
        val a = 100.0.pixels()
        val b = 200.0.pixels()

        // 덧셈
        (a + a) shouldBeEqualTo b
        // 뺄셈
        (b - a) shouldBeEqualTo a
        // 스칼라 곱셈
        (a * 2) shouldBeEqualTo b
        // 스칼라 나눗셈
        (b / 2) shouldBeEqualTo a
    }

    // ----- 비교 -----

    @Test
    fun `픽셀 비교 연산이 동작한다`() {
        200.pixels() shouldBeGreaterThan 100.pixels()
        50.pixels() shouldBeLessThan 100.pixels()
    }

    // ----- 음수 단위 -----

    @Test
    fun `픽셀 음수 단위가 동작한다`() {
        val positive = 100.pixels()
        val negative = -positive

        (negative `in` GraphicsLength.pixels).shouldBeNear(-100.0, 1e-10)

        val zero = positive + negative
        (zero `in` GraphicsLength.pixels).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `픽셀 toString 표현이 단위를 포함한다`() {
        100.0.pixels().toString() shouldBeEqualTo "100.0 px"
        1920.0.pixels().toString() shouldBeEqualTo "1920.0 px"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1080.pixels().toHuman(GraphicsLength.pixels) shouldBeEqualTo "1080.0 px"
    }
}
