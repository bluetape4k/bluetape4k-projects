package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class AreaTest {

    companion object: KLogging()

    @Test
    fun `길이 곱셈으로 면적을 계산한다`() {
        val area = 10.meters() * 2.meters()
        (area `in` Area.meters2).shouldBeNear(20.0, 1e-10)
    }

    @Test
    fun `면적 단위 변환이 동작한다`() {
        val squareMeter = 1.meters2()
        (squareMeter `in` Area.centimeters2).shouldBeNear(10_000.0, 1e-7)
    }

    @Test
    fun `면적 toHuman 이 자동 단위를 선택한다`() {
        10_000.centimeters2().toHuman() shouldBeEqualTo "1.0 m^2"
    }

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        val m2 = 1.meters2()
        val asCm2 = m2 `as` Area.centimeters2
        (asCm2 `in` Area.centimeters2).shouldBeNear(10_000.0, 1e-7)

        val km2 = 1.kilometers2()
        val asM2 = km2 `as` Area.meters2
        (asM2 `in` Area.meters2).shouldBeNear(1_000_000.0, 1e-4)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (1.meters2() `in` Area.millimeters2).shouldBeNear(1_000_000.0, 1e-5)
        (1.meters2() `in` Area.centimeters2).shouldBeNear(10_000.0, 1e-7)
        (1.kilometers2() `in` Area.meters2).shouldBeNear(1_000_000.0, 1e-4)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `면적 사칙연산이 동작한다`() {
        val a = 50.0.meters2()
        val b = 100.0.meters2()

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
    fun `면적 사칙연산 - 다른 단위 혼합`() {
        val cm2 = 5000.centimeters2()  // 0.5 m^2
        val m2 = 1.meters2()

        val sum = cm2 + m2
        (sum `in` Area.meters2).shouldBeNear(1.5, 1e-10)

        val diff = m2 - cm2
        (diff `in` Area.meters2).shouldBeNear(0.5, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `면적 비교 연산이 동작한다`() {
        2.meters2() shouldBeGreaterThan 1.meters2()
        1.kilometers2() shouldBeGreaterThan 999999.meters2()
        1.centimeters2() shouldBeLessThan 1.meters2()

        // 다른 단위 간 비교
        1.meters2() shouldBeGreaterThan 9999.centimeters2()
        1.kilometers2() shouldBeGreaterThan 1.meters2()
    }

    // ----- 음수 단위 -----

    @Test
    fun `면적 음수 단위가 동작한다`() {
        val positive = 100.meters2()
        val negative = -positive

        (negative `in` Area.meters2).shouldBeNear(-100.0, 1e-10)

        val zero = positive + negative
        (zero `in` Area.meters2).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `면적 toString 표현이 단위를 포함한다`() {
        100.0.meters2().toString() shouldBeEqualTo "100.0 m^2"
        1.5.kilometers2().toString() shouldBeEqualTo "1.5 km^2"
        500.0.centimeters2().toString() shouldBeEqualTo "500.0 cm^2"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1.meters2().toHuman(Area.meters2) shouldBeEqualTo "1.0 m^2"
        10000.centimeters2().toHuman(Area.meters2) shouldBeEqualTo "1.0 m^2"
        1000000.meters2().toHuman(Area.kilometers2) shouldBeEqualTo "1.0 km^2"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.millimeters2().toHuman() shouldBeEqualTo "5.0 cm^2"
        10000.centimeters2().toHuman() shouldBeEqualTo "1.0 m^2"
        2000000.meters2().toHuman() shouldBeEqualTo "2.0 km^2"
    }
}
