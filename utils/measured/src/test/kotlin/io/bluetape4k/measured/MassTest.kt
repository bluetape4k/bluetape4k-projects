package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class MassTest {

    companion object: KLogging()

    @Test
    fun `질량 단위 변환이 동작한다`() {
        val ton = 2.tons()

        (ton `in` Mass.kilograms).shouldBeNear(2000.0, 1e-10)
        (ton `in` Mass.grams).shouldBeNear(2_000_000.0, 1e-5)
    }

    @Test
    fun `질량 toHuman 이 자동 단위를 선택한다`() {
        2500.grams().toHuman() shouldBeEqualTo "2.5 kg"
    }

    // ----- 사칙 연산 -----

    @Test
    fun `질량 사칙연산이 동작한다`() {
        val a = 500.0.grams()
        val b = 1000.0.grams()

        // 덧셈
        (a + a) shouldBeEqualTo b
        // 뺄셈
        (b - a) shouldBeEqualTo a
        // 스칼라 곱셈
        (a * 2) shouldBeEqualTo b
        // 스칼라 나눗셈
        (b / 2) shouldBeEqualTo a
    }

    @Test
    fun `질량 사칙연산 - 다른 단위 혼합`() {
        val g = 500.grams()
        val kg = 1.kilograms()

        val sum = g + kg
        (sum `in` Mass.grams).shouldBeNear(1500.0, 1e-10)

        val diff = kg - g
        (diff `in` Mass.grams).shouldBeNear(500.0, 1e-10)
    }

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        // 1000 g -> kg
        val grams = 1000.grams()
        val asKg = grams `as` Mass.kilograms
        (asKg `in` Mass.kilograms).shouldBeNear(1.0, 1e-10)

        // 1 ton -> kg
        val ton = 1.tons()
        val asKg2 = ton `as` Mass.kilograms
        (asKg2 `in` Mass.kilograms).shouldBeNear(1000.0, 1e-10)

        // 2.5 kg -> g
        val kg = 2.5.kilograms()
        val asGrams = kg `as` Mass.grams
        (asGrams `in` Mass.grams).shouldBeNear(2500.0, 1e-10)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (1.kilograms() `in` Mass.grams).shouldBeNear(1000.0, 1e-10)
        (1.tons() `in` Mass.kilograms).shouldBeNear(1000.0, 1e-10)
        (500.grams() `in` Mass.kilograms).shouldBeNear(0.5, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `질량 비교 연산이 동작한다`() {
        2.kilograms() shouldBeGreaterThan 1.kilograms()
        1.tons() shouldBeGreaterThan 999.kilograms()
        500.grams() shouldBeLessThan 1.kilograms()

        // 다른 단위 간 비교
        1.kilograms() shouldBeGreaterThan 999.grams()
        1.tons() shouldBeGreaterThan 1.kilograms()
    }

    // ----- 음수 단위 -----

    @Test
    fun `질량 음수 단위가 동작한다`() {
        val positive = 500.grams()
        val negative = -positive

        (negative `in` Mass.grams).shouldBeNear(-500.0, 1e-10)

        // 양수 + 음수 = 0
        val zero = positive + negative
        (zero `in` Mass.grams).shouldBeNear(0.0, 1e-10)

        // 음수 kg
        val negKg = (-2).kilograms()
        (negKg `in` Mass.grams).shouldBeNear(-2000.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `질량 toString 표현이 단위를 포함한다`() {
        500.0.grams().toString() shouldBeEqualTo "500.0 g"
        1.5.kilograms().toString() shouldBeEqualTo "1.5 kg"
        2.0.tons().toString() shouldBeEqualTo "2.0 ton"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1000.grams().toHuman(Mass.grams) shouldBeEqualTo "1000.0 g"
        1000.grams().toHuman(Mass.kilograms) shouldBeEqualTo "1.0 kg"
        2000.kilograms().toHuman(Mass.tons) shouldBeEqualTo "2.0 ton"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.grams().toHuman() shouldBeEqualTo "500.0 g"
        2500.grams().toHuman() shouldBeEqualTo "2.5 kg"
        3000.kilograms().toHuman() shouldBeEqualTo "3.0 ton"
    }
}
