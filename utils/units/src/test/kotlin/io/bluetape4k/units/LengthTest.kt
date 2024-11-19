package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RandomizedTest
class LengthTest {

    companion object: KLogging()

    @Test
    fun `convert length unit`() {
        100.meter().inMeter() shouldBeEqualTo 100.0
        100.kilometer().inMeter() shouldBeEqualTo 100.0 * 1e3
        100.millimeter().inMeter() shouldBeEqualTo 100.0 * 1e-3
    }

    @Test
    fun `convert length unit by random`(@RandomValue(type = Double::class) lengths: List<Double>) {
        lengths.forEach { length ->
            length.meter().inMeter() shouldBeEqualTo length
            length.meter().inKilometer() shouldBeEqualTo length / 1e3
        }
    }

    @Test
    fun `convert human expression`() {
        100.meter().toHuman() shouldBeEqualTo "100.0 m"
        123.millimeter().toHuman() shouldBeEqualTo "1.2 cm"
        422.centimeter().toHuman() shouldBeEqualTo "42.2 m"
        123.43.kilometer().toHuman() shouldBeEqualTo "123.4 km"
    }

    @Test
    fun `parse with null or blank string to NaN`() {
        Length.parse(null) shouldBeEqualTo Length.NaN
        Length.parse("") shouldBeEqualTo Length.NaN
        Length.parse(" \t ") shouldBeEqualTo Length.NaN
    }

    @Test
    fun `parse length expression`() {
        Length.parse("100 m") shouldBeEqualTo 100.meter()
        Length.parse("17.5 mm") shouldBeEqualTo 17.5.millimeter()
        Length.parse("8.1 km") shouldBeEqualTo 8.1.kilometer()
        Length.parse("8.1 cm") shouldBeEqualTo 8.1.centimeter()
        Length.parse("8.1 cms") shouldBeEqualTo 8.1.centimeter()
    }

    @Test
    fun `parse invalid expression`() {
        assertFailsWith<NumberFormatException> {
            Length.parse("9.1")
        }
        assertFailsWith<NumberFormatException> {
            Length.parse("9.1.1")
        }
        assertFailsWith<NumberFormatException> {
            Length.parse("9.1 kmeter")
        }
        assertFailsWith<NumberFormatException> {
            Length.parse("9.1 millis")
        }
        assertFailsWith<NumberFormatException> {
            Length.parse("9.1.0.1 MMs")
        }
    }

    @Test
    fun `length negative`() {
        (-100).millimeter() shouldBeEqualTo lengthOf(-100.0)
        -(100.meter()) shouldBeEqualTo lengthOf(-100.0 * 1e3)
    }

    @Test
    fun `length oprators`() {
        val a = 100.0.meter()
        val b = 200.0.meter()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `compare length`() {
        assertTrue { 1.78.kilometer() > 1.7.kilometer() }
        assertTrue { 1.78.meter() > 1.2.meter() }
        assertTrue { 123.millimeter() < 0.9.meter() }
    }
}
