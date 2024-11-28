package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
            length.meter().inMeter().shouldBeNear(length, EPSILON)
            length.meter().inKilometer().shouldBeNear(length / 1.0e3, EPSILON)
        }
    }

    @Test
    fun `convert human expression`() {
        100.meter().toHuman() shouldBeEqualTo "100.0 m"
        123.millimeter().toHuman() shouldBeEqualTo "12.3 cm"
        422.centimeter().toHuman() shouldBeEqualTo "4.2 m"
        123.43.kilometer().toHuman() shouldBeEqualTo "123.4 km"
    }

    @Test
    fun `convert to specific unit expression`() {
        100.meter().toHuman(LengthUnit.METER) shouldBeEqualTo "100.0 m"
        123.millimeter().toHuman(LengthUnit.CENTIMETER) shouldBeEqualTo "12.3 cm"

        422.centimeter().toHuman(LengthUnit.METER) shouldBeEqualTo "4.2 m"
        123.43.kilometer().toHuman(LengthUnit.METER) shouldBeEqualTo "123430.0 m"
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
        assertFailsWith<IllegalArgumentException> {
            Length.parse("9.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Length.parse("9.1.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Length.parse("9.1 kmeter")
        }
        assertFailsWith<IllegalArgumentException> {
            Length.parse("9.1 millis")
        }
        assertFailsWith<IllegalArgumentException> {
            Length.parse("9.1.0.1 MMs")
        }
    }

    @Test
    fun `length negative`() {
        (-100).millimeter() shouldBeEqualTo lengthOf(-100.0 * LengthUnit.MILLIMETER.factor)
        -(100.meter()) shouldBeEqualTo lengthOf(-100.0 * LengthUnit.METER.factor)
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
    fun `convertTo new length unit`() {
        100.meter().convertTo(LengthUnit.METER) shouldBeEqualTo 100.meter()
        100.meter().convertTo(LengthUnit.KILOMETER) shouldBeEqualTo 0.1.kilometer()
        100.meter().convertTo(LengthUnit.CENTIMETER) shouldBeEqualTo (100 * 100).centimeter()
        100.meter().convertTo(LengthUnit.MILLIMETER) shouldBeEqualTo (100 * 1000).millimeter()
    }

    @Test
    fun `compare length`() {
        1.78.kilometer() shouldBeGreaterThan 1.7.kilometer()
        1.78.meter() shouldBeGreaterThan 1.2.meter()
        123.millimeter() shouldBeLessThan 0.9.meter()
    }
}
