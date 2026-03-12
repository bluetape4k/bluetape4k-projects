package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class TimeTest {

    companion object: KLogging()

    @Test
    fun `시간 기반 복합 단위 계산이 동작한다`() {
        val speed: Measure<Velocity> = 10.meters() / 2.seconds()
        val distance = speed * 6.seconds()

        (distance `in` Length.meters).shouldBeNear(30.0, 1e-10)
    }

    @Test
    fun `시간 toHuman 이 자동 단위를 선택한다`() {
        120000.milliseconds().toHuman() shouldBeEqualTo "2.0 min"
    }

    // ----- 단위 변환 -----

    @Test
    fun `시간 단위 변환이 동작한다`() {
        (1.hours() `in` Time.minutes).shouldBeNear(60.0, 1e-10)
        (1.hours() `in` Time.seconds).shouldBeNear(3600.0, 1e-10)
        (1.hours() `in` Time.milliseconds).shouldBeNear(3_600_000.0, 1e-5)
        (1.minutes() `in` Time.seconds).shouldBeNear(60.0, 1e-10)
        (1.minutes() `in` Time.milliseconds).shouldBeNear(60_000.0, 1e-8)
        (1.seconds() `in` Time.milliseconds).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        val twoHours = 2.hours()
        val asMinutes = twoHours `as` Time.minutes
        (asMinutes `in` Time.minutes).shouldBeNear(120.0, 1e-10)

        val threeMinutes = 3.minutes()
        val asSeconds = threeMinutes `as` Time.seconds
        (asSeconds `in` Time.seconds).shouldBeNear(180.0, 1e-10)

        val fiveSeconds = 5.seconds()
        val asMs = fiveSeconds `as` Time.milliseconds
        (asMs `in` Time.milliseconds).shouldBeNear(5000.0, 1e-10)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `시간 사칙연산이 동작한다`() {
        val a = 30.0.seconds()
        val b = 60.0.seconds()

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
    fun `시간 사칙연산 - 다른 단위 혼합`() {
        val sec = 30.seconds()
        val min = 1.minutes()

        val sum = sec + min
        (sum `in` Time.seconds).shouldBeNear(90.0, 1e-10)

        val diff = min - sec
        (diff `in` Time.seconds).shouldBeNear(30.0, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `시간 비교 연산이 동작한다`() {
        2.hours() shouldBeGreaterThan 1.hours()
        1.minutes() shouldBeGreaterThan 59.seconds()
        1.seconds() shouldBeGreaterThan 999.milliseconds()

        // 다른 단위 간 비교
        1.hours() shouldBeGreaterThan 59.minutes()
        1.minutes() shouldBeLessThan 61.seconds()
        500.milliseconds() shouldBeLessThan 1.seconds()
    }

    // ----- 음수 단위 -----

    @Test
    fun `시간 음수 단위가 동작한다`() {
        val positive = 10.seconds()
        val negative = -positive

        (negative `in` Time.seconds).shouldBeNear(-10.0, 1e-10)

        val zero = positive + negative
        (zero `in` Time.seconds).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `시간 toString 표현이 단위를 포함한다`() {
        60.0.seconds().toString() shouldBeEqualTo "60.0 s"
        1.5.minutes().toString() shouldBeEqualTo "1.5 min"
        2.0.hours().toString() shouldBeEqualTo "2.0 hr"
        500.0.milliseconds().toString() shouldBeEqualTo "500.0 ms"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        3600.seconds().toHuman(Time.hours) shouldBeEqualTo "1.0 hr"
        60.seconds().toHuman(Time.minutes) shouldBeEqualTo "1.0 min"
        1000.milliseconds().toHuman(Time.seconds) shouldBeEqualTo "1.0 s"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.milliseconds().toHuman() shouldBeEqualTo "500.0 ms"
        3000.milliseconds().toHuman() shouldBeEqualTo "3.0 s"
        120000.milliseconds().toHuman() shouldBeEqualTo "2.0 min"
        7200000.milliseconds().toHuman() shouldBeEqualTo "2.0 hr"
    }
}
