package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class TemperatureTest {

    companion object: KLogging()

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

    // ----- 단위 변환 -----

    @Test
    fun `온도 단위 변환이 올바르다`() {
        // Kelvin <-> Celsius
        273.15.kelvin().inCelsius().shouldBeNear(0.0, 1e-10)
        100.celsius().inKelvin().shouldBeNear(373.15, 1e-10)

        // Celsius <-> Fahrenheit
        0.celsius().inFahrenheit().shouldBeNear(32.0, 1e-10)
        100.celsius().inFahrenheit().shouldBeNear(212.0, 1e-10)

        // Fahrenheit <-> Celsius
        212.fahrenheit().inCelsius().shouldBeNear(100.0, 1e-10)
        32.fahrenheit().inCelsius().shouldBeNear(0.0, 1e-10)

        // Fahrenheit -> Kelvin
        32.fahrenheit().inKelvin().shouldBeNear(273.15, 1e-10)
    }

    @Test
    fun `온도 변환 라운드트립이 동작한다`() {
        val original = 25.0
        val celsius = original.celsius()

        celsius.inCelsius().shouldBeNear(original, 1e-10)
        celsius.inKelvin().shouldBeNear(original + 273.15, 1e-10)
        Temperature.fromKelvin(celsius.inKelvin()).inCelsius().shouldBeNear(original, 1e-10)
    }

    // ----- 사칙 연산 (TemperatureDelta) -----

    @Test
    fun `온도 차이 사칙연산이 동작한다`() {
        val base = 20.celsius()

        // 더하기
        val raised = base + 15.celsiusDelta()
        raised.inCelsius().shouldBeNear(35.0, 1e-10)

        // 빼기
        val lowered = base - 5.celsiusDelta()
        lowered.inCelsius().shouldBeNear(15.0, 1e-10)

        // 두 온도의 차이
        val delta = raised - lowered
        delta.inCelsius().shouldBeNear(20.0, 1e-10)
    }

    @Test
    fun `Fahrenheit 온도 차이 연산이 동작한다`() {
        val base = 32.fahrenheit()   // 0°C
        val raised = base + 9.fahrenheitDelta()  // 9°F 차이 = 5°C 차이

        raised.inFahrenheit().shouldBeNear(41.0, 1e-10)
        raised.inCelsius().shouldBeNear(5.0, 1e-10)
    }

    // ----- 음수 TemperatureDelta -----

    @Test
    fun `음수 온도 차이가 동작한다`() {
        val base = 25.celsius()
        val negativeDelta = (-10).celsiusDelta()

        val result = base + negativeDelta
        result.inCelsius().shouldBeNear(15.0, 1e-10)

        // 음수 Kelvin 온도 (절대온도 0 이하는 물리적으로 불가하나 수치적으로 동작)
        val negKelvin = (-10).kelvinDelta()
        negKelvin.inKelvin().shouldBeNear(-10.0, 1e-10)
        negKelvin.inCelsius().shouldBeNear(-10.0, 1e-10)
        negKelvin.inFahrenheit().shouldBeNear(-18.0, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `온도 비교 연산이 동작한다`() {
        100.celsius() shouldBeGreaterThan 0.celsius()
        (-10).celsius() shouldBeLessThan 0.celsius()
        373.15.kelvin() shouldBeGreaterThan 273.15.kelvin()

        // 다른 단위 간 비교: 100K < 373.15K (100°C)
        100.celsius() shouldBeGreaterThan 100.kelvin()
        // 10°C(283.15K) > 32°F(273.15K)
        10.celsius() shouldBeGreaterThan 32.fahrenheit()
        // 240°F(388.7K) > 36.5°C(309.65K)
        240.fahrenheit() shouldBeGreaterThan 36.5.celsius()
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 모든 단위 표현이 올바르다`() {
        0.celsius().toHuman(TemperatureUnit.CELSIUS) shouldBeEqualTo "0.0 °C"
        0.celsius().toHuman(TemperatureUnit.KELVIN) shouldBeEqualTo "273.15 K"
        0.celsius().toHuman(TemperatureUnit.FAHRENHEIT) shouldBeEqualTo "32.0 °F"

        100.celsius().toHuman(TemperatureUnit.CELSIUS) shouldBeEqualTo "100.0 °C"
        100.celsius().toHuman(TemperatureUnit.FAHRENHEIT) shouldBeEqualTo "212.0 °F"
    }

    @Test
    fun `TemperatureDelta toHuman 이 동작한다`() {
        10.celsiusDelta().toHuman(TemperatureUnit.CELSIUS) shouldBeEqualTo "10.0 °C"
        10.kelvinDelta().toHuman(TemperatureUnit.KELVIN) shouldBeEqualTo "10.0 K"
        10.kelvinDelta().toHuman(TemperatureUnit.FAHRENHEIT) shouldBeEqualTo "18.0 °F"
    }

    // ----- toString 표현 -----

    @Test
    fun `온도 toString 표현이 올바르다`() {
        // toString uses Celsius by default
        25.celsius().toString() shouldBeEqualTo "25.0 °C"
        0.celsius().toString() shouldBeEqualTo "0.0 °C"
    }
}
