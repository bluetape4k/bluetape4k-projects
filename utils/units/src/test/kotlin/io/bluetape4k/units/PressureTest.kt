package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.random.Random

@RandomizedTest
class PressureTest {

    companion object: KLogging()

    @Test
    fun `convert pressure unit`() {
        1.0.pascal().inPascal() shouldBeEqualTo 1.0 * PressureUnit.PASCAL.factor / PressureUnit.PASCAL.factor
        1.0.pascal().inHectoPascal() shouldBeEqualTo 1.0 * PressureUnit.PASCAL.factor / PressureUnit.HECTO_PASCAL.factor
        1.0.pascal().inKiloPascal() shouldBeEqualTo 1.0 * PressureUnit.PASCAL.factor / PressureUnit.KILO_PASCAL.factor
        1.0.pascal().inMegaPascal() shouldBeEqualTo 1.0 * PressureUnit.PASCAL.factor / PressureUnit.MEGA_PASCAL.factor
        1.0.pascal().inMegaPascal() shouldBeEqualTo 1.0 * PressureUnit.PASCAL.factor / PressureUnit.MEGA_PASCAL.factor

        1.0.psi().inAtm() shouldBeEqualTo 1.0 * PressureUnit.PSI.factor / PressureUnit.ATM.factor
        1.0.psi().inBar() shouldBeEqualTo 1.0 * PressureUnit.PSI.factor / PressureUnit.BAR.factor
    }

    @Test
    fun `convert pressure unit by random`() {
        val pressures = List(100) { Random.nextDouble(-100.0, 100.0) }

        pressures.forEach { pressure ->
            pressure.pascal().inPascal().shouldBeNear(pressure, EPSILON)
            pressure.pascal().inHectoPascal().shouldBeNear(pressure / 1.0e2, EPSILON)
            pressure.pascal().inKiloPascal().shouldBeNear(pressure / 1.0e3, EPSILON)
            pressure.pascal().inMegaPascal().shouldBeNear(pressure / 1.0e6, EPSILON)

            pressure.pascal().inTorr().shouldBeNear(pressure / PressureUnit.TORR.factor, EPSILON)
        }
    }

    @Test
    fun `convert human expression`() {
        100.0.psi().toHuman() shouldBeEqualTo "84350278.8 mmHg"
    }

    @Test
    fun `convert to specific unit expression`() {
        12345.6.pascal().toHuman(PressureUnit.KILO_PASCAL) shouldBeEqualTo "12.3 kPa"
        12345678.9.pascal().toHuman(PressureUnit.MEGA_PASCAL) shouldBeEqualTo "12.3 mPa"

        100.psi().toHuman(PressureUnit.ATM) shouldBeEqualTo "6.8 atm"
        100.psi().toHuman(PressureUnit.BAR) shouldBeEqualTo "6.9 bar"
    }

    @Test
    fun `convert to other pressure unit`() {
        123.4.pascal().convertTo(PressureUnit.PASCAL) shouldBeEqualTo 123.4.pascal()
        1234.5.pascal().convertTo(PressureUnit.HECTO_PASCAL) shouldBeEqualTo 12.345.hectoPascal()

        38.psi().convertTo(PressureUnit.BAR).value.shouldBeNear(2.6.bar().value, 1e4)
        2.6.bar().convertTo(PressureUnit.PSI).value.shouldBeNear(37.7.psi().value, 1e4)
    }

    @Test
    fun `parse with null or blank string to NaN`() {
        Pressure.parse(null) shouldBeEqualTo Pressure.NaN
        Pressure.parse("") shouldBeEqualTo Pressure.NaN
        Pressure.parse(" \t ") shouldBeEqualTo Pressure.NaN
    }

    @Test
    fun `parse pressure expression`() {
        Pressure.parse("100 Pa") shouldBeEqualTo 100.pascal()
        Pressure.parse("17.5 psi") shouldBeEqualTo 17.5.psi()
        Pressure.parse("8.1 kPa") shouldBeEqualTo 8.1.kiloPascal()
        Pressure.parse("8.1 bar") shouldBeEqualTo 8.1.bar()
        Pressure.parse("8.1 bars") shouldBeEqualTo 8.1.bar()
    }

    @Test
    fun `parse invalid expression`() {
        assertFailsWith<IllegalArgumentException> {
            Pressure.parse("9.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Pressure.parse("9.1.1")
        }
    }

    @Test
    fun `parse invalid unit expression`() {
        assertFailsWith<IllegalArgumentException> {
            Pressure.parse("9.1 pascal")
        }
        assertFailsWith<IllegalArgumentException> {
            Pressure.parse("9.1 mPax")
        }
        assertFailsWith<IllegalArgumentException> {
            Pressure.parse("9.1 gPass1")
        }
    }

    @Test
    fun `negative pressure`() {
        (-100).pascal() shouldBeEqualTo pressureOf(-100.0 * PressureUnit.PASCAL.factor)
        -(100.psi()) shouldBeEqualTo pressureOf(-100.0 * PressureUnit.PSI.factor)
    }

    @Test
    fun `basic operators`() {
        val a = 100.pascal()
        val b = 200.pascal()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `convertTo new pressure unit`() {
        100.pascal().convertTo(PressureUnit.PASCAL) shouldBeEqualTo 100.pascal()
        100.pascal().convertTo(PressureUnit.KILO_PASCAL) shouldBeEqualTo 0.1.kiloPascal()
        100.pascal().convertTo(PressureUnit.MEGA_PASCAL) shouldBeEqualTo 0.0001.megaPascal()
    }

    @Test
    fun `compare pressure`() {
        100.pascal() shouldBeGreaterThan 10.pascal()
        1.kiloPascal() shouldBeGreaterThan 100.0.pascal()
        100.pascal() shouldBeLessThan 100.0.psi()
        26.0.bar() shouldBeLessThan 400.0.psi()
    }
}
