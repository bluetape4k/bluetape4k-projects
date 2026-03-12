package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class MotionTest {

    companion object: KLogging()

    @Test
    fun `속도 확장 함수와 변환이 동작한다`() {
        val speed = 36.kilometersPerHour()
        speed.inMetersPerSecond().shouldBeNear(10.0, 1e-10)
    }

    @Test
    fun `가속도 확장 함수와 변환이 동작한다`() {
        val acceleration = 9.8.metersPerSecondSquared()
        acceleration.inMetersPerSecondSquared().shouldBeNear(9.8, 1e-10)
    }

    // ----- 속도 단위 변환 -----

    @Test
    fun `속도 단위 변환이 동작한다`() {
        // 36 km/h = 10 m/s
        36.kilometersPerHour().inMetersPerSecond().shouldBeNear(10.0, 1e-10)
        // 10 m/s = 36 km/h
        10.metersPerSecond().inKilometersPerHour().shouldBeNear(36.0, 1e-10)
        // 100 km/h -> m/s
        100.kilometersPerHour().inMetersPerSecond().shouldBeNear(27.778, 1e-3)
    }

    @Test
    fun `as 연산자로 속도 단위를 변환한다`() {
        val kmh = 36.kilometersPerHour()
        val asMs = kmh `as` MotionUnits.metersPerSecond
        (asMs `in` MotionUnits.metersPerSecond).shouldBeNear(10.0, 1e-10)
    }

    // ----- 속도 사칙 연산 -----

    @Test
    fun `속도 사칙연산이 동작한다`() {
        val a = 5.0.metersPerSecond()
        val b = 10.0.metersPerSecond()

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
    fun `속도와 시간으로 거리를 계산한다`() {
        val speed = 10.metersPerSecond()
        val time = 5.seconds()
        val distance = speed * time

        (distance `in` Length.meters).shouldBeNear(50.0, 1e-10)
    }

    // ----- 속도 비교 -----

    @Test
    fun `속도 비교 연산이 동작한다`() {
        20.metersPerSecond() shouldBeGreaterThan 10.metersPerSecond()
        100.kilometersPerHour() shouldBeGreaterThan 27.metersPerSecond()   // 100 km/h ≈ 27.78 m/s
        36.kilometersPerHour() shouldBeEqualTo 10.metersPerSecond()       // 정확히 같은 속도

        10.metersPerSecond() shouldBeLessThan 40.kilometersPerHour()  // 10 m/s = 36 km/h < 40 km/h
    }

    // ----- 속도 음수 -----

    @Test
    fun `속도 음수 단위가 동작한다`() {
        val forward = 10.metersPerSecond()
        val backward = -forward

        backward.inMetersPerSecond().shouldBeNear(-10.0, 1e-10)

        val zero = forward + backward
        (zero `in` MotionUnits.metersPerSecond).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `속도 toString 표현이 단위를 포함한다`() {
        10.0.metersPerSecond().toString() shouldBeEqualTo "10.0 m/s"
        36.0.kilometersPerHour().toString() shouldBeEqualTo "36.0 km/hr"
    }

    // ----- 가속도 사칙 연산 -----

    @Test
    fun `가속도 사칙연산이 동작한다`() {
        val a = 5.0.metersPerSecondSquared()
        val b = 10.0.metersPerSecondSquared()

        (a + a) shouldBeEqualTo b
        (b - a) shouldBeEqualTo a
        (a * 2) shouldBeEqualTo b
        (b / 2) shouldBeEqualTo a
    }

    // ----- 가속도 비교 -----

    @Test
    fun `가속도 비교 연산이 동작한다`() {
        9.8.metersPerSecondSquared() shouldBeGreaterThan 9.0.metersPerSecondSquared()
        1.0.metersPerSecondSquared() shouldBeLessThan 9.8.metersPerSecondSquared()
    }

    // ----- 가속도 음수 -----

    @Test
    fun `가속도 음수 단위가 동작한다`() {
        val a = 9.8.metersPerSecondSquared()
        val negA = -a

        negA.inMetersPerSecondSquared().shouldBeNear(-9.8, 1e-10)
    }

    // ----- 가속도 toString -----

    @Test
    fun `가속도 toString 표현이 단위를 포함한다`() {
        9.8.metersPerSecondSquared().toString() shouldBeEqualTo "9.8 m/(s)^2"
    }
}
