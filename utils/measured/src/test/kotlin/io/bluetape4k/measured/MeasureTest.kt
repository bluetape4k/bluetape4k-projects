package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class MeasureTest {
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
    fun `복합 단위 계산이 동작한다`() {
        val speed: Measure<Velocity> = 10.meters() / 2.seconds()
        val distance = speed * 6.seconds()

        (distance `in` Length.meters).shouldBeNear(30.0, 1e-10)
    }

    @Test
    fun `질량 단위 변환이 동작한다`() {
        val ton = 2.tons()

        (ton `in` Mass.kilograms).shouldBeNear(2000.0, 1e-10)
        (ton `in` Mass.grams).shouldBeNear(2_000_000.0, 1e-5)
    }

    @Test
    fun `toNearest가 동작한다`() {
        val rounded = 10.26.meters().toNearest(0.1)
        (rounded `in` Length.meters).shouldBeNear(10.3, 1e-10)
    }

    @Test
    fun `문자열 표현이 단위를 포함한다`() {
        val value = 42.kilograms()
        value.toString() shouldBeEqualTo "42.0 kg"
    }
}
