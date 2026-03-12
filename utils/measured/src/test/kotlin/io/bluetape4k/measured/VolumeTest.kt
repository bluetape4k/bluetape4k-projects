package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class VolumeTest {

    companion object: KLogging()

    @Test
    fun `부피 단위 변환이 동작한다`() {
        (1.liters() `in` Volume.milliliters).shouldBeNear(1000.0, 1e-10)
        (1.cubicMeters() `in` Volume.liters).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `면적과 길이로 부피를 계산한다`() {
        val volume = 10.meters2() * 2.meters()
        (volume `in` Volume.cubicMeters).shouldBeNear(20.0, 1e-10)
    }

    @Test
    fun `부피 toHuman 이 자동 단위를 선택한다`() {
        1500.milliliters().toHuman() shouldBeEqualTo "1.5 L"
    }

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        val liter = 1.liters()
        val asMl = liter `as` Volume.milliliters
        (asMl `in` Volume.milliliters).shouldBeNear(1000.0, 1e-10)

        val m3 = 1.cubicMeters()
        val asLiters = m3 `as` Volume.liters
        (asLiters `in` Volume.liters).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (1.cubicMeters() `in` Volume.liters).shouldBeNear(1000.0, 1e-10)
        (1.cubicMeters() `in` Volume.milliliters).shouldBeNear(1_000_000.0, 1e-5)
        (1.liters() `in` Volume.cubicCentimeters).shouldBeNear(1000.0, 1e-10)
        (1.liters() `in` Volume.cubicMillimeters).shouldBeNear(1_000_000.0, 1e-5)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `부피 사칙연산이 동작한다`() {
        val a = 500.0.milliliters()
        val b = 1000.0.milliliters()

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
    fun `부피 사칙연산 - 다른 단위 혼합`() {
        val ml = 500.milliliters()
        val l = 1.liters()

        val sum = ml + l
        (sum `in` Volume.milliliters).shouldBeNear(1500.0, 1e-10)

        val diff = l - ml
        (diff `in` Volume.milliliters).shouldBeNear(500.0, 1e-10)
    }

    // ----- 부피 역연산 -----

    @Test
    fun `부피를 면적으로 나누면 길이가 나온다`() {
        val volume = 20.cubicMeters()
        val area = 10.meters2()
        val length = volume / area

        (length `in` Length.meters).shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `부피를 길이로 나누면 면적이 나온다`() {
        val volume = 20.cubicMeters()
        val height = 4.meters()
        val area = volume / height

        (area `in` Area.meters2).shouldBeNear(5.0, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `부피 비교 연산이 동작한다`() {
        2.liters() shouldBeGreaterThan 1.liters()
        1.cubicMeters() shouldBeGreaterThan 999.liters()
        500.milliliters() shouldBeLessThan 1.liters()

        // 다른 단위 간 비교
        1.liters() shouldBeGreaterThan 999.milliliters()
        1.cubicMeters() shouldBeGreaterThan 1.liters()
    }

    // ----- 음수 단위 -----

    @Test
    fun `부피 음수 단위가 동작한다`() {
        val positive = 1.liters()
        val negative = -positive

        (negative `in` Volume.milliliters).shouldBeNear(-1000.0, 1e-10)

        val zero = positive + negative
        (zero `in` Volume.milliliters).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `부피 toString 표현이 단위를 포함한다`() {
        1000.0.milliliters().toString() shouldBeEqualTo "1000.0 mL"
        1.5.liters().toString() shouldBeEqualTo "1.5 L"
        2.0.cubicMeters().toString() shouldBeEqualTo "2.0 m^3"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1000.milliliters().toHuman(Volume.liters) shouldBeEqualTo "1.0 L"
        1.cubicMeters().toHuman(Volume.liters) shouldBeEqualTo "1000.0 L"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.cubicMillimeters().toHuman() shouldBeEqualTo "500.0 mm^3"
        1500.milliliters().toHuman() shouldBeEqualTo "1.5 L"
        2000.liters().toHuman() shouldBeEqualTo "2.0 m^3"
    }
}
