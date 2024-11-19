package io.bluetape4k.units

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions
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
    fun `convert weight unit by random`(@RandomValue(type = Double::class) weights: List<Double>) {
        weights.forEach { weight ->
            weight.gram().inGram() shouldBeEqualTo weight
            weight.gram().inKillogram() shouldBeEqualTo weight / 1e3
        }
    }

    @Test
    fun `make weight list`(@RandomValue(type = Double::class) weights: List<Double>) {
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
        assertFailsWith<NumberFormatException> {
            Weight.parse("9.1")
        }
        assertFailsWith<NumberFormatException> {
            Weight.parse("9.1 bytes")
        }
        assertFailsWith<NumberFormatException> {
            Weight.parse("9.1 Bytes")
        }
        assertFailsWith<NumberFormatException> {
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
        Assertions.assertTrue { 1.78.kilogram() > 1.7.kilogram() }
        Assertions.assertTrue { 1.78.gram() > 1.2.gram() }
        Assertions.assertTrue { 123.kilogram() < 0.9.ton() }
    }
}
