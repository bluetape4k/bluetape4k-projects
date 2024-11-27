package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.units.TemperatureUnit.CELSIUS
import io.bluetape4k.units.TemperatureUnit.FAHRENHEIT
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test

@RandomizedTest
class TemperatureTest {

    companion object: KLogging()

    @Test
    fun `convert temperature unit`() {
        100.kelvin().inKelvin() shouldBeEqualTo 100.0
        100.celsius().inKelvin() shouldBeEqualTo 100.0 + 273.15
        100.fahrenheit().inKelvin() shouldBeEqualTo 100.0 + 459.67
    }

    @Test
    fun `convert temperature unit by random`(@RandomValue(type = Double::class) temperatures: List<Double>) {
        temperatures.forEach { temperature ->
            temperature.kelvin().inKelvin() shouldBeEqualTo temperature
            temperature.celsius().inKelvin() shouldBeEqualTo temperature + 273.15
            temperature.fahrenheit().inKelvin() shouldBeEqualTo temperature + 459.67
        }
    }

    @Test
    fun `convert human expression`() {
        100.kelvin().toHuman() shouldBeEqualTo "100.0 K"
        36.5.celsius().toHuman(CELSIUS) shouldBeEqualTo "36.5 °C"
        422.9.fahrenheit().toHuman(FAHRENHEIT) shouldBeEqualTo "422.9 °F"
    }

    @Test
    fun `parse with null or blank string to NaN`() {
        Temperature.parse(null) shouldBeEqualTo Temperature.NaN
        Temperature.parse("") shouldBeEqualTo Temperature.NaN
        Temperature.parse(" \t ") shouldBeEqualTo Temperature.NaN
    }

    @Test
    fun `parse temperature expression`() {
        Temperature.parse("100 K") shouldBeEqualTo 100.kelvin()
        Temperature.parse("17.5 °C") shouldBeEqualTo 17.5.celsius()
        Temperature.parse("224.3 °F") shouldBeEqualTo 224.3.fahrenheit()
    }

    @Test
    fun `parse invalid expression`() {
        assertFailsWith<IllegalArgumentException> {
            Temperature.parse("9.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Temperature.parse("9.1.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Temperature.parse("9.1 KK")
        }
        assertFailsWith<IllegalArgumentException> {
            Temperature.parse("9.1 C")
        }
        assertFailsWith<IllegalArgumentException> {
            Temperature.parse("9.1.0.1 C")
        }
    }

    @Test
    fun `temperature negative`() {
        (-100).celsius() shouldBeEqualTo temperatureOf(-100.0, CELSIUS)
        -(100.fahrenheit()) shouldBeEqualTo temperatureOf(100.0, FAHRENHEIT).unaryMinus()
    }

    @Test
    fun `temperature oprators`() {
        // NOTE: Kelvin 만 가능하고, 다른 온도 단위로 연산하면 예외 발생한다 (온도 단위가 다르기 때문)
        val a = 100.0.kelvin()
        val b = 200.0.kelvin()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `compare temperature`() {
        100.kelvin() shouldBeLessThan 0.celsius()
        10.celsius() shouldBeLessThan 100.fahrenheit()
        240.fahrenheit() shouldBeGreaterThan 36.5.celsius()
    }
}
