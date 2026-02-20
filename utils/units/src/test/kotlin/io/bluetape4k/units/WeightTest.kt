package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@RandomizedTest
class WeightTest {
    companion object: KLogging()

    @Test
    fun `convert weight unit`() {
        100.gram().inGram() shouldBeEqualTo 100.0
        100.kilogram().inGram() shouldBeEqualTo 100.0 * 1e3
        100.milligram().inGram() shouldBeEqualTo 100.0 * 1e-3
    }

    @Test
    fun `convert weight unit by random`(
        @RandomValue(type = Double::class) weights: List<Double>,
    ) {
        weights.forEach { weight ->
            weight.gram().inGram() shouldBeEqualTo weight
            weight.gram().inKilogram() shouldBeEqualTo weight / 1e3
        }
    }

    @Test
    fun `make weight list`(
        @RandomValue(type = Double::class) weights: List<Double>,
    ) {
        val list = weights.map { Weight(it) }
        list.size shouldBeEqualTo weights.size
        list[0].value shouldBeEqualTo weights[0]
    }

    @Test
    fun `convert human expression`() {
        100.gram().toHuman() shouldBeEqualTo "100.0 g"
        123.milligram().toHuman() shouldBeEqualTo "123.0 mg"
        123.43.kilogram().toHuman() shouldBeEqualTo "123.4 kg"
        12.59.ton().toHuman() shouldBeEqualTo "12.6 ton"
    }

    @Test
    fun `parse with null or blank string to NaN`() {
        Weight.parse(null) shouldBeEqualTo Weight.NaN
        Weight.parse("") shouldBeEqualTo Weight.NaN
        Weight.parse(" \t ") shouldBeEqualTo Weight.NaN
    }

    @Test
    fun `parse weight expression`() {
        Weight.parse("100 g") shouldBeEqualTo 100.gram()
        Weight.parse("17.5 mg") shouldBeEqualTo 17.5.milligram()
        Weight.parse("8.1 kg") shouldBeEqualTo 8.1.kilogram()
        Weight.parse("8.1 ton") shouldBeEqualTo 8.1.ton()
        Weight.parse("8.1 tons") shouldBeEqualTo 8.1.ton()
    }

    @Test
    fun `parse invalid expression`() {
        assertFailsWith<IllegalArgumentException> {
            Weight.parse("9.1")
        }
        assertFailsWith<IllegalArgumentException> {
            Weight.parse("9.1 bytes")
        }
        assertFailsWith<IllegalArgumentException> {
            Weight.parse("9.1 Bytes")
        }
        assertFailsWith<IllegalArgumentException> {
            Weight.parse("9.1.0.1 B")
        }
    }

    @Test
    fun `weight neative`() {
        (-100).gram() shouldBeEqualTo weightOf(-100.0)
        -(100.gram()) shouldBeEqualTo weightOf(-100.0)
    }

    @Test
    fun `weight oprators`() {
        val a = 100.0.kilogram()
        val b = 200.0.kilogram()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `compare weight`() {
        1.78.kilogram() shouldBeGreaterThan 1.7.kilogram()
        1.78.gram() shouldBeGreaterThan 1.2.gram()
        123.kilogram() shouldBeLessThan 0.9.ton()
    }

    @Test
    fun `weight constants`() {
        Weight.ZERO.value shouldBeEqualTo 0.0
        Weight.NaN.value.isNaN().shouldBeTrue()
    }

    @Test
    fun `weight convertTo`() {
        1000.0.gram().convertTo(WeightUnit.KILOGRAM).inKilogram() shouldBeEqualTo 1.0
        1.0.ton().convertTo(WeightUnit.GRAM).inGram() shouldBeEqualTo 1e6
        500.0.milligram().convertTo(WeightUnit.GRAM).inGram() shouldBeEqualTo 0.5
    }

    @Test
    fun `weight toHuman with different scales`() {
        // 매우 작은 무게
        0.5.milligram().toHuman() shouldBeEqualTo "0.5 mg"

        // 매우 큰 무게
        1000.0.ton().toHuman() shouldBeEqualTo "1000.0 ton"

        // 음수 무게
        (-5.0).kilogram().toHuman() shouldBeEqualTo "-5.0 kg"
    }

    @Test
    fun `weight all unit conversions`() {
        val weight = 1.0.kilogram()
        weight.inMilligram() shouldBeEqualTo 1e6
        weight.inGram() shouldBeEqualTo 1e3
        weight.inKilogram() shouldBeEqualTo 1.0
        weight.inTon() shouldBeEqualTo 1e-3
    }
}
