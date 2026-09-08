package io.bluetape4k.measured

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

@RandomizedTest
class ElectricalTest {

    companion object: KLogging()

    @Test
    fun `전류와 전하의 SI 접두어를 변환한다`() {
        (1000.milliAmps() `in` Current.amps).shouldBeNear(1.0, 1e-10)
        (1.kiloAmps() `in` Current.amps).shouldBeNear(1000.0, 1e-10)
        (1.coulombs() `in` Charge.milliCoulombs).shouldBeNear(1000.0, 1e-10)
        (1.milliCoulombs() `in` Charge.microCoulombs).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `전압과 저항의 SI 접두어를 변환한다`() {
        (1000.milliVolts() `in` Voltage.volts).shouldBeNear(1.0, 1e-10)
        (1.kiloVolts() `in` Voltage.volts).shouldBeNear(1000.0, 1e-10)
        (1000.milliOhms() `in` Resistance.ohms).shouldBeNear(1.0, 1e-10)
        (1.megaOhms() `in` Resistance.ohms).shouldBeNear(1_000_000.0, 1e-4)
    }

    @Test
    fun `전류와 시간으로 전하를 계산하고 역연산한다`() {
        val charge = 1.amps() * 1.seconds()

        (charge `in` Charge.coulombs).shouldBeNear(1.0, 1e-10)
        val current = charge / 1.seconds()
        (current `in` Current.amps).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `전력과 전류로 전압을 계산하고 혼합 스케일을 정규화한다`() {
        val voltage = 2.kiloWatts() / 500.milliAmps()

        (voltage `in` Voltage.volts).shouldBeNear(4_000.0, 1e-8)
        (voltage / 500.milliAmps() `in` Resistance.ohms).shouldBeNear(8_000.0, 1e-6)
    }

    @Test
    fun `전압과 전류로 저항을 계산한다`() {
        val resistance = 1.volts() / 1.amps()

        (resistance `in` Resistance.ohms).shouldBeNear(1.0, 1e-10)
        (1.kiloVolts() / 1.milliAmps() `in` Resistance.ohms).shouldBeNear(1_000_000.0, 1e-4)
    }

    @Test
    fun `전기 단위의 사칙연산과 비교가 동작한다`() {
        val oneAmp = 1.amps()
        val halfAmp = 500.milliAmps()

        (oneAmp + halfAmp `in` Current.amps).shouldBeNear(1.5, 1e-10)
        (oneAmp - halfAmp `in` Current.milliAmps).shouldBeNear(500.0, 1e-8)
        (oneAmp * 2 `in` Current.amps).shouldBeNear(2.0, 1e-10)
        (oneAmp / 2 `in` Current.milliAmps).shouldBeNear(500.0, 1e-8)
        oneAmp shouldBeGreaterThan halfAmp
        halfAmp shouldBeLessThan oneAmp
    }

    @Test
    fun `전기 단위 toHuman 이 접두어와 기호를 보존한다`() {
        1500.amps().toHuman() shouldBeEqualTo "1.5 kA"
        500.milliAmps().toHuman() shouldBeEqualTo "500.0 mA"
        0.5.volts().toHuman() shouldBeEqualTo "500.0 mV"
        1_500_000.ohms().toHuman() shouldBeEqualTo "1.5 MΩ"
        0.milliCoulombs().toHuman() shouldBeEqualTo "0.0 μC"
        (-1).kiloVolts().toHuman() shouldBeEqualTo "-1.0 kV"
    }

    @Test
    fun `전기 단위의 toString 이 단위를 포함한다`() {
        1.0.amps().toString() shouldBeEqualTo "1.0 A"
        2.0.milliCoulombs().toString() shouldBeEqualTo "2.0 mC"
        3.0.kiloVolts().toString() shouldBeEqualTo "3.0 kV"
        4.0.megaOhms().toString() shouldBeEqualTo "4.0 MΩ"
    }
}
