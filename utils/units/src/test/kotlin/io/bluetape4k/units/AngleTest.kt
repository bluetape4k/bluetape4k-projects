package io.bluetape4k.units

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class AngleTest {

    companion object: KLogging()

    @Test
    fun `convert angle unit`() {
        90.degree().inDegree() shouldBeEqualTo 90.0
        90.degree().inRadian() shouldBeEqualTo 90.0 * Math.PI / 180.0

        Math.PI.radian().inRadian() shouldBeEqualTo Math.PI
        Math.PI.radian().inDegree() shouldBeEqualTo 180.0
    }

    @Test
    fun `convert angle unit by random`() {
        val degrees = sequence {
            while (true) {
                yield((Math.random() * 360).degree())
            }
        }
        degrees.take(100).forEach { angle ->
            angle.inDegree().shouldBeNear(angle.value, EPSILON)
            angle.inRadian().shouldBeNear(angle.value * Math.PI / 180.0, EPSILON)
        }
    }

    @Test
    fun `generate human expression`() {
        90.degree().toHuman() shouldBeEqualTo "90.0 deg"
        180.degree().toHuman() shouldBeEqualTo "180.0 deg"
        360.degree().toHuman() shouldBeEqualTo "0.0 deg"
        720.degree().toHuman() shouldBeEqualTo "0.0 deg"

        Math.PI.radian().toHuman() shouldBeEqualTo "180.0 deg"
        (2 * Math.PI).radian().toHuman() shouldBeEqualTo "0.0 deg"
    }

    @Test
    fun `convert to specific unit expression`() {
        90.degree().toHuman(AngleUnit.DEGREE) shouldBeEqualTo "90.0 deg"
        90.degree().toHuman(AngleUnit.RADIAN) shouldBeEqualTo "1.6 rad"

        Math.PI.radian().toHuman(AngleUnit.DEGREE) shouldBeEqualTo "180.0 deg"
        Math.PI.radian().toHuman(AngleUnit.RADIAN) shouldBeEqualTo "3.1 rad"
    }

    @Test
    fun `parse with null or blank string to NaN`() {
        Angle.parse(null) shouldBeEqualTo Angle.NaN
        Angle.parse("") shouldBeEqualTo Angle.NaN
        Angle.parse(" \t ") shouldBeEqualTo Angle.NaN
    }

    @Test
    fun `parse invalid expression`() {
        assertFailsWith<IllegalArgumentException> {
            Angle.parse("9.1")
        }

        assertFailsWith<IllegalArgumentException> {
            Angle.parse("9.1.1")
        }

        assertFailsWith<IllegalArgumentException> {
            Angle.parse("180 degree")
        }
        assertFailsWith<IllegalArgumentException> {
            Angle.parse("2.1 radian")
        }

        Angle.parse("180.0 degs")
        Angle.parse("3.1 rads")
    }

    @Test
    fun `negative angle`() {
        (-90).degree().inDegree() shouldBeEqualTo -90.0
        (-90).degree().inRadian() shouldBeEqualTo -90.0 / AngleUnit.RADIAN.factor
    }

    @Test
    fun `arithmetic operators for angle`() {
        val a = 90.degree()
        val b = 180.degree()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `convertTo new angle unit`() {
        180.degree().convertTo(AngleUnit.RADIAN) shouldBeEqualTo Math.PI.radian()
        Math.PI.radian().convertTo(AngleUnit.DEGREE) shouldBeEqualTo 180.0.degree()
    }

    @Test
    fun `compare angle`() {
        180.degree() shouldBeGreaterThan 90.degree()
        180.degree() shouldBeGreaterThan 1.radian()
    }
}
