package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.PI

@RandomizedTest
class AngleTest {

    companion object: KLogging()

    @Test
    fun `각도 단위 변환이 동작한다`() {
        (180.degrees() `in` Angle.radians).shouldBeNear(PI, 1e-10)
        (PI.radians() `in` Angle.degrees).shouldBeNear(180.0, 1e-10)
    }

    @Test
    fun `각도 정규화가 동작한다`() {
        (450.degrees().normalize() `in` Angle.degrees).shouldBeNear(90.0, 1e-10)
        ((-30).degrees().normalize() `in` Angle.degrees).shouldBeNear(330.0, 1e-10)
    }

    @Test
    fun `각도 toHuman 이 정규화된 degree 로 표시된다`() {
        450.degrees().toHuman() shouldBeEqualTo "90.0°"
    }

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        val halfCircle = 180.degrees()
        val asRadians = halfCircle `as` Angle.radians
        (asRadians `in` Angle.radians).shouldBeNear(PI, 1e-10)

        val piRadians = PI.radians()
        val asDegrees = piRadians `as` Angle.degrees
        (asDegrees `in` Angle.degrees).shouldBeNear(180.0, 1e-10)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (90.degrees() `in` Angle.radians).shouldBeNear(PI / 2, 1e-10)
        (360.degrees() `in` Angle.radians).shouldBeNear(2 * PI, 1e-10)
        ((PI / 2).radians() `in` Angle.degrees).shouldBeNear(90.0, 1e-10)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `각도 사칙연산이 동작한다`() {
        val a = 90.0.degrees()
        val b = 180.0.degrees()

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
    fun `각도 사칙연산 - 다른 단위 혼합`() {
        val deg = 90.degrees()
        val rad = (PI / 2).radians()

        // 90° + π/2 rad = 180°
        val sum = deg + rad
        (sum `in` Angle.degrees).shouldBeNear(180.0, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `각도 비교 연산이 동작한다`() {
        180.degrees() shouldBeGreaterThan 90.degrees()
        90.degrees() shouldBeLessThan 180.degrees()
        PI.radians() shouldBeGreaterThan (PI / 2).radians()

        // 다른 단위 간 비교
        180.degrees() shouldBeGreaterThan (PI / 3).radians()  // 180° > 60°
        (PI / 2).radians() shouldBeLessThan 180.degrees()    // 90° < 180°
    }

    // ----- 음수 단위 -----

    @Test
    fun `각도 음수 단위가 동작한다`() {
        val positive = 90.degrees()
        val negative = -positive

        (negative `in` Angle.degrees).shouldBeNear(-90.0, 1e-10)

        val zero = positive + negative
        (zero `in` Angle.degrees).shouldBeNear(0.0, 1e-10)

        // 음수는 정규화 시 양수로 변환
        ((-90).degrees().normalize() `in` Angle.degrees).shouldBeNear(270.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `각도 toString 표현이 단위를 포함한다`() {
        // 도(°)는 spaceBetweenMagnitude = false 이므로 공백 없음
        90.0.degrees().toString() shouldBeEqualTo "90.0°"
        1.0.radians().toString() shouldBeEqualTo "1.0 rad"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        90.degrees().toHuman(Angle.degrees) shouldBeEqualTo "90.0°"
        // formatHuman 은 소수점 9자리로 반올림: PI ≈ 3.141592654
        (180.degrees() `in` Angle.radians).shouldBeNear(PI, 1e-9)
    }

    @Test
    fun `toHuman 은 0-360도 범위로 정규화한다`() {
        0.degrees().toHuman() shouldBeEqualTo "0.0°"
        360.degrees().toHuman() shouldBeEqualTo "0.0°"
        720.degrees().toHuman() shouldBeEqualTo "0.0°"
        (-90).degrees().toHuman() shouldBeEqualTo "270.0°"
    }

    // ----- 삼각함수 -----

    @Test
    fun `삼각함수가 동작한다`() {
        Angle.sin(90.degrees()).shouldBeNear(1.0, 1e-10)
        Angle.cos(0.degrees()).shouldBeNear(1.0, 1e-10)
        Angle.tan(45.degrees()).shouldBeNear(1.0, 1e-10)

        // 역삼각함수
        (Angle.asin(1.0) `in` Angle.degrees).shouldBeNear(90.0, 1e-10)
        (Angle.acos(1.0) `in` Angle.degrees).shouldBeNear(0.0, 1e-10)
        (Angle.atan(1.0) `in` Angle.degrees).shouldBeNear(45.0, 1e-10)
    }
}
