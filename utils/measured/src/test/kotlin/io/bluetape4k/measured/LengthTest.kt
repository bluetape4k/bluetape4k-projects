package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class LengthTest {

    companion object: KLogging()

    @Test
    fun `길이 단위 변환이 동작한다`() {
        val km = 1.kilometers()

        (km `in` Length.meters).shouldBeNear(1000.0, 1e-10)
        (km `in` Length.millimeters).shouldBeNear(1_000_000.0, 1e-6)
    }

    @Test
    fun `동일 계열 측정값 덧셈이 동작한다`() {
        val left = 500.meters()
        val right = 1.kilometers()

        val sum = left + right
        (sum `in` Length.meters).shouldBeNear(1500.0, 1e-10)
    }

    @Test
    fun `길이 toHuman 이 자동 단위를 선택한다`() {
        1500.meters().toHuman() shouldBeEqualTo "1.5 km"
    }

    // ----- 사칙 연산 -----

    @Test
    fun `길이 사칙연산이 동작한다`() {
        val a = 100.0.meters()
        val b = 200.0.meters()

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
    fun `길이 사칙연산 - 다른 단위 혼합`() {
        val m = 500.meters()
        val km = 1.kilometers()

        val sum = m + km
        (sum `in` Length.meters).shouldBeNear(1500.0, 1e-10)

        val diff = km - m
        (diff `in` Length.meters).shouldBeNear(500.0, 1e-10)
    }

    // ----- 단위 변환 (km -> meter 등) -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        // 100 m -> km
        val hundredMeters = 100.meters()
        val asKm = hundredMeters `as` Length.kilometers
        (asKm `in` Length.kilometers).shouldBeNear(0.1, 1e-10)

        // 1 km -> m
        val oneKm = 1.kilometers()
        val asMeters = oneKm `as` Length.meters
        (asMeters `in` Length.meters).shouldBeNear(1000.0, 1e-10)

        // 1000 mm -> m
        val mm = 1000.millimeters()
        val asMeter = mm `as` Length.meters
        (asMeter `in` Length.meters).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (100.meters() `in` Length.centimeters).shouldBeNear(10000.0, 1e-8)
        (1.kilometers() `in` Length.meters).shouldBeNear(1000.0, 1e-10)
        (1.miles() `in` Length.meters).shouldBeNear(1609.344, 1e-3)
        (1.feet() `in` Length.meters).shouldBeNear(0.3048, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `길이 비교 연산이 동작한다`() {
        1.78.kilometers() shouldBeGreaterThan 1.7.kilometers()
        1.78.meters() shouldBeGreaterThan 1.2.meters()
        123.millimeters() shouldBeLessThan 0.9.meters()

        // 다른 단위 간 비교
        1.kilometers() shouldBeGreaterThan 999.meters()
        1.meters() shouldBeGreaterThan 999.millimeters()
        1.inches() shouldBeLessThan 1.feet()
    }

    // ----- 음수 단위 -----

    @Test
    fun `길이 음수 단위가 동작한다`() {
        val positive = 100.meters()
        val negative = -positive

        (negative `in` Length.meters).shouldBeNear(-100.0, 1e-10)

        // 음수 + 양수 = 0
        val zero = positive + negative
        (zero `in` Length.meters).shouldBeNear(0.0, 1e-10)

        // 음수 km
        val negKm = (-1).kilometers()
        (negKm `in` Length.meters).shouldBeNear(-1000.0, 1e-10)
    }

    // ----- toString 표현 (parse 미지원으로 toString으로 대체) -----

    @Test
    fun `길이 toString 표현이 단위를 포함한다`() {
        100.0.meters().toString() shouldBeEqualTo "100.0 m"
        1.5.kilometers().toString() shouldBeEqualTo "1.5 km"
        500.0.millimeters().toString() shouldBeEqualTo "500.0 mm"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        100.meters().toHuman(Length.meters) shouldBeEqualTo "100.0 m"
        100.meters().toHuman(Length.kilometers) shouldBeEqualTo "0.1 km"
        1000.meters().toHuman(Length.kilometers) shouldBeEqualTo "1.0 km"
        0.1.kilometers().toHuman(Length.meters) shouldBeEqualTo "100.0 m"
        100.meters().toHuman(Length.centimeters) shouldBeEqualTo "10000.0 cm"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        0.5.meters().toHuman() shouldBeEqualTo "50.0 cm"
        1500.meters().toHuman() shouldBeEqualTo "1.5 km"
        0.005.meters().toHuman() shouldBeEqualTo "5.0 mm"
    }
}
