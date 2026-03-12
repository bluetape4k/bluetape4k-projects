package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class EnergyPowerTest {

    companion object: KLogging()

    @Test
    fun `에너지 단위 변환이 동작한다`() {
        (1.kiloWattHours() `in` Energy.joules).shouldBeNear(3_600_000.0, 1e-5)
        (3_600.joules() `in` Energy.wattHours).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `전력과 시간으로 에너지를 계산한다`() {
        val energy = 2.kiloWatts() * 3.hours()
        (energy `in` Energy.kiloWattHours).shouldBeNear(6.0, 1e-10)

        val power = energy / 2.hours()
        (power `in` Power.kiloWatts).shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `에너지 전력 toHuman 이 자동 단위를 선택한다`() {
        1500.joules().toHuman() shouldBeEqualTo "1.5 kJ"
        1500.watts().toHuman() shouldBeEqualTo "1.5 kW"
    }

    // ----- 에너지 단위 변환 -----

    @Test
    fun `에너지 as 연산자로 다른 단위로 변환한다`() {
        val kj = 1.kiloJoules()
        val asJ = kj `as` Energy.joules
        (asJ `in` Energy.joules).shouldBeNear(1000.0, 1e-10)

        val kwh = 1.kiloWattHours()
        val asWh = kwh `as` Energy.wattHours
        (asWh `in` Energy.wattHours).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `에너지 in 연산자로 단위 수치값을 추출한다`() {
        (1.kiloJoules() `in` Energy.joules).shouldBeNear(1000.0, 1e-10)
        (1.megaJoules() `in` Energy.kiloJoules).shouldBeNear(1000.0, 1e-10)
        (1.wattHours() `in` Energy.joules).shouldBeNear(3600.0, 1e-10)
        (1.kiloWattHours() `in` Energy.wattHours).shouldBeNear(1000.0, 1e-10)
    }

    // ----- 에너지 사칙 연산 -----

    @Test
    fun `에너지 사칙연산이 동작한다`() {
        val a = 500.0.joules()
        val b = 1000.0.joules()

        (a + a) shouldBeEqualTo b
        (b - a) shouldBeEqualTo a
        (a * 2) shouldBeEqualTo b
        (b / 2) shouldBeEqualTo a
    }

    @Test
    fun `에너지 사칙연산 - 다른 단위 혼합`() {
        val j = 500.joules()
        val kj = 1.kiloJoules()

        val sum = j + kj
        (sum `in` Energy.joules).shouldBeNear(1500.0, 1e-10)

        val diff = kj - j
        (diff `in` Energy.joules).shouldBeNear(500.0, 1e-10)
    }

    // ----- 에너지 비교 -----

    @Test
    fun `에너지 비교 연산이 동작한다`() {
        2.kiloJoules() shouldBeGreaterThan 1.kiloJoules()
        1.kiloJoules() shouldBeGreaterThan 999.joules()
        1.wattHours() shouldBeGreaterThan 1.kiloJoules()   // 3600 J > 1000 J

        1.joule() shouldBeLessThan 1.wattHours()
        1.kiloJoules() shouldBeLessThan 1.wattHours()      // 1000 J < 3600 J
    }

    // ----- 에너지 음수 단위 -----

    @Test
    fun `에너지 음수 단위가 동작한다`() {
        val positive = 1.kiloJoules()
        val negative = -positive

        (negative `in` Energy.joules).shouldBeNear(-1000.0, 1e-10)

        val zero = positive + negative
        (zero `in` Energy.joules).shouldBeNear(0.0, 1e-10)
    }

    // ----- 전력 단위 변환 -----

    @Test
    fun `전력 as 연산자로 다른 단위로 변환한다`() {
        val kw = 1.kiloWatts()
        val asW = kw `as` Power.watts
        (asW `in` Power.watts).shouldBeNear(1000.0, 1e-10)

        val mw = 1.megaWatts()
        val asKw = mw `as` Power.kiloWatts
        (asKw `in` Power.kiloWatts).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `전력 in 연산자로 단위 수치값을 추출한다`() {
        (1.kiloWatts() `in` Power.watts).shouldBeNear(1000.0, 1e-10)
        (1.megaWatts() `in` Power.kiloWatts).shouldBeNear(1000.0, 1e-10)
        (1000.milliWatts() `in` Power.watts).shouldBeNear(1.0, 1e-10)
    }

    // ----- 전력 사칙 연산 -----

    @Test
    fun `전력 사칙연산이 동작한다`() {
        val a = 500.0.watts()
        val b = 1000.0.watts()

        (a + a) shouldBeEqualTo b
        (b - a) shouldBeEqualTo a
        (a * 2) shouldBeEqualTo b
        (b / 2) shouldBeEqualTo a
    }

    // ----- 전력 비교 -----

    @Test
    fun `전력 비교 연산이 동작한다`() {
        2.kiloWatts() shouldBeGreaterThan 1.kiloWatts()
        1.megaWatts() shouldBeGreaterThan 999.kiloWatts()
        1.watts() shouldBeLessThan 1.kiloWatts()
        500.milliWatts() shouldBeLessThan 1.watts()
    }

    // ----- 전력 음수 단위 -----

    @Test
    fun `전력 음수 단위가 동작한다`() {
        val positive = 1.kiloWatts()
        val negative = -positive

        (negative `in` Power.watts).shouldBeNear(-1000.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `에너지 toString 표현이 단위를 포함한다`() {
        1000.0.joules().toString() shouldBeEqualTo "1000.0 J"
        1.5.kiloJoules().toString() shouldBeEqualTo "1.5 kJ"
        2.0.wattHours().toString() shouldBeEqualTo "2.0 Wh"
    }

    @Test
    fun `전력 toString 표현이 단위를 포함한다`() {
        500.0.watts().toString() shouldBeEqualTo "500.0 W"
        1.5.kiloWatts().toString() shouldBeEqualTo "1.5 kW"
        2.0.megaWatts().toString() shouldBeEqualTo "2.0 MW"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `에너지 toHuman 에 특정 단위를 지정할 수 있다`() {
        1000.joules().toHuman(Energy.kiloJoules) shouldBeEqualTo "1.0 kJ"
        3600.joules().toHuman(Energy.wattHours) shouldBeEqualTo "1.0 Wh"
    }

    @Test
    fun `에너지 toHuman 자동 단위 선택이 올바르다`() {
        500.joules().toHuman() shouldBeEqualTo "500.0 J"
        1500.joules().toHuman() shouldBeEqualTo "1.5 kJ"
    }

    @Test
    fun `전력 toHuman 에 특정 단위를 지정할 수 있다`() {
        1000.watts().toHuman(Power.kiloWatts) shouldBeEqualTo "1.0 kW"
        1000.kiloWatts().toHuman(Power.megaWatts) shouldBeEqualTo "1.0 MW"
    }

    @Test
    fun `전력 toHuman 자동 단위 선택이 올바르다`() {
        500.milliWatts().toHuman() shouldBeEqualTo "500.0 mW"
        1500.watts().toHuman() shouldBeEqualTo "1.5 kW"
        2500.kiloWatts().toHuman() shouldBeEqualTo "2.5 MW"
    }
}

// 1 J extension for Energy (단수 표기)
private fun Number.joule(): Measure<Energy> = this.joules()
