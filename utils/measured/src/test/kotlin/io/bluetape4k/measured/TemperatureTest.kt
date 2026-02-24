package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class TemperatureTest {
    @Test
    fun `절대 온도 변환이 동작한다`() {
        0.celsius().inKelvin().shouldBeNear(273.15, 1e-10)
        32.fahrenheit().inCelsius().shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `온도 차이 연산이 동작한다`() {
        val base = 25.celsius()
        val raised = base + 10.celsiusDelta()

        raised.inCelsius().shouldBeNear(35.0, 1e-10)
        (raised - base).inCelsius().shouldBeNear(10.0, 1e-10)
    }

    @Test
    fun `온도 toHuman 이 동작한다`() {
        25.celsius().toHuman() shouldBeEqualTo "25.0 °C"
        298.15.kelvin().toHuman(TemperatureUnit.KELVIN) shouldBeEqualTo "298.15 K"
    }
}
