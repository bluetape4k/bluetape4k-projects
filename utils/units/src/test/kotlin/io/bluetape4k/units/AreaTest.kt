package io.bluetape4k.units

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AreaTest {
    companion object: KLogging()

    @Test
    fun `convert area`() {
        100.0.millimeter2().inMillimeter2() shouldBeEqualTo 100.0
        100.0.centimeter2().inCentimeter2() shouldBeEqualTo 100.0
        100.0.meter2().inMeter2() shouldBeEqualTo 100.0
    }

    @Test
    fun `display human string`() {
        100.0.millimeter2().toHuman() shouldBeEqualTo "100.0 mm^2"
        100.0.centimeter2().toHuman() shouldBeEqualTo "100.0 cm^2"
        100.0.meter2().toHuman() shouldBeEqualTo "100.0 m^2"
    }

    @Test
    fun `parse empty string`() {
        Area.parse(null) shouldBeEqualTo Area.NaN
        Area.parse("") shouldBeEqualTo Area.NaN
        Area.parse(" \t ") shouldBeEqualTo Area.NaN
    }

    @Test
    fun `parse valid string`() {
        Area.parse("144.4 mm^2") shouldBeEqualTo 144.4.millimeter2()
        Area.parse("144.4 cm^2") shouldBeEqualTo 144.4.centimeter2()
        Area.parse("144.4 m^2") shouldBeEqualTo 144.4.meter2()
    }

    @Test
    fun `parse invalid format`() {
        assertFailsWith<IllegalArgumentException> {
            Area.parse("123.4")
        }
        assertFailsWith<IllegalArgumentException> {
            Area.parse("123.4 MC^2")
        }
        assertFailsWith<IllegalArgumentException> {
            Area.parse("123.4.4.0.0 m^2")
        }
    }

    @Test
    fun `negate expression`() {
        144.0.meter2().inMeter2().unaryMinus() shouldBeEqualTo -144.0
        144.0.millimeter2().inMeter2().unaryMinus() shouldBeEqualTo -144.0 * 1e-6
    }

    @Test
    fun `area arithmetic`() {
        val a = 100.meter2()
        val b = 200.meter2()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `compare area`() {
        1.78.meter2() shouldBeGreaterThan 1.7.meter2()
        1.0.meter2() shouldBeGreaterThan 99.0.centimeter2()
        1.0.meter2() shouldBeLessThan 10001.0.centimeter2()
    }

    @Test
    fun `area constants`() {
        Area.ZERO.value shouldBeEqualTo 0.0
        Area.NaN.value.isNaN() shouldBeEqualTo true
        Area.MaxValue.value shouldBeEqualTo Double.MAX_VALUE
        Area.MinValue.value shouldBeEqualTo Double.MIN_VALUE
        Area.PositiveInf.value shouldBeEqualTo Double.POSITIVE_INFINITY
        Area.NegateInf.value shouldBeEqualTo Double.NEGATIVE_INFINITY
    }

    @Test
    fun `area with length operations`() {
        // 면적 * 길이 = 부피
        val area = 2.0.meter2()
        val length = 3.0.meter()
        val volume = area * length
        volume.inMeter3() shouldBeEqualTo 6.0

        // 면적 / 길이 = 길이
        val resultLength = area / length
        resultLength.inMeter() shouldBeEqualTo 2.0 / 3.0
    }

    @Test
    fun `area convertTo`() {
        100.0.meter2().convertTo(AreaUnit.CENTI_METER_2).inCentimeter2() shouldBeEqualTo 100.0 * 1e4
        10000.0.centimeter2().convertTo(AreaUnit.METER_2).inMeter2() shouldBeEqualTo 1.0
    }
}
