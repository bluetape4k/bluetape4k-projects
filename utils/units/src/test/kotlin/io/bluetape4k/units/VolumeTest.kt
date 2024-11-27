package io.bluetape4k.units

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class VolumeTest {

    companion object: KLogging()

    @Test
    fun `convert Volume unit`() {
        100.liter().inCC() shouldBeEqualTo 100.0 * 1e9
        100.liter().inCentiMeter3() shouldBeEqualTo 100.0 * 1e3
        100.liter().inMeter3() shouldBeEqualTo 100.0 * 1e-3

        1000.milliliter().inLiter() shouldBeEqualTo 1.0
        1000.deciliter().inLiter() shouldBeEqualTo 10.0
    }

    @Test
    fun `to human string`() {
        100.liter().toHuman() shouldBeEqualTo "100.0 l"
        10.deciliter().toHuman(VolumeUnit.DECILITER) shouldBeEqualTo "10.0 dl"
        100.milliliter().toHuman(VolumeUnit.DECILITER) shouldBeEqualTo "10.0 dl"
    }

    @Test
    fun `parse nothing`() {
        Volume.parse(null) shouldBeEqualTo Volume.NaN
        Volume.parse("") shouldBeEqualTo Volume.NaN
        Volume.parse(" \t ") shouldBeEqualTo Volume.NaN
    }

    @Test
    fun `parse valid volume expression`() {
        Volume.parse("123 l") shouldBeEqualTo 123.liter()
        Volume.parse("156.7 ml") shouldBeEqualTo 156.7.milliliter()
        Volume.parse("15.4 m^3") shouldBeEqualTo 15.4.meter3()
    }

    @Test
    fun `parse invalid volume expression`() {
        assertFailsWith<IllegalArgumentException> {
            Volume.parse("1.0")
        }
        assertFailsWith<IllegalArgumentException> {
            Volume.parse("1.0 ll")
        }
        assertFailsWith<IllegalArgumentException> {
            Volume.parse("ml 1.0")
        }
        assertFailsWith<IllegalArgumentException> {
            Volume.parse("1.0.0.0 ml")
        }
    }

    @Test
    fun `negate volume`() {
        (-1.5).liter().inLiter() shouldBeEqualTo -1.5
    }

    @Test
    fun `arithmetic operators for volume`() {
        val a = volumeOf(100.0)
        val b = volumeOf(200.0)

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `compare Volume`() {
        1.7.liter() shouldBeGreaterThan 169.deciliter()
        1.6.liter() shouldBeLessThan 169.deciliter()
        1.2.deciliter() shouldBeGreaterThan 11.milliliter()
        1.2.deciliter() shouldBeLessThan 12.1.milliliter()
    }
}
