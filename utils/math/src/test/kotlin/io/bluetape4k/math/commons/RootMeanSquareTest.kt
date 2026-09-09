package io.bluetape4k.math.commons

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.random.Random

@RandomizedTest
class RootMeanSquareTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Nested
    inner class RMS {

        @Test
        fun `rms uses the arithmetic mean of squares for singleton and signed values`() {
            sequenceOf(3.0, 4.0).rms().shouldBeNear(sqrt(12.5), 1e-10)
            sequenceOf(-3.0, -4.0).rms().shouldBeNear(sqrt(12.5), 1e-10)
            sequenceOf(3.0).rms().shouldBeNear(3.0, 1e-10)
        }

        @Test
        fun `rms for empty values`() {
            emptyList<Double>().rms() shouldBeEqualTo 0.0
            doubleArrayOf().asSequence().rms() shouldBeEqualTo 0.0
        }

        @Test
        fun `rms for identity values`() {
            val values = List(10) { 1.0 }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms.shouldBeNear(1.0, 1e-10)
        }

        @Test
        fun `rms for incremental values`() {
            val values = List(10) { it }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms.shouldBeNear(sqrt(values.map { it.toDouble().square() }.average()), 1e-10)
        }

        @Test
        fun `rms for square wave`() {
            val values = List(10) { if (it % 2 == 0) 0.0 else 1.0 }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms.shouldBeNear(sqrt(0.5), 1e-10)
        }

        @Test
        fun `rms for sin wave`() {
            val values = List(10) { sin(it.toDouble()) }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms.shouldBeNear(sqrt(values.map { it.square() }.average()), 1e-10)
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `rms for elements vs reversed elements`(@RandomValue size: Int) {
            val length = size.abs().coerceIn(10, 100)
            val values1 = List(length) { Random.nextInt(0, 100) }
            val values2 = values1.reversed()

            val rms1 = values1.rms()
            val rms2 = values2.rms()
            log.trace { "rms=$rms1, reversed rms=$rms2" }
            rms1 shouldBeEqualTo rms2
        }
    }

    @Nested
    inner class RMSE {

        @Test
        fun `rmse consumes each sequence once and uses the arithmetic mean`() {
            val expected = sequenceOf(2.0, 4.0).constrainOnce()
            val actual = sequenceOf(1.0, 1.0).constrainOnce()

            expected.rmse(actual).shouldBeNear(sqrt(5.0), 1e-10)
        }

        @Test
        fun `rmse rejects sequences with different lengths`() {
            assertFailsWith<IllegalArgumentException> {
                sequenceOf(1.0, 2.0).rmse(sequenceOf(1.0))
            }
            assertFailsWith<IllegalArgumentException> {
                sequenceOf(1.0).rmse(sequenceOf(1.0, 2.0))
            }
        }

        @Test
        fun `rmse for empty values`() {
            emptyList<Double>().rmse(emptyList()) shouldBeEqualTo 0.0
        }

        @Test
        fun `rmse for identity values`() {
            val values = List(10) { 1.0 }
            val rmse = values.rmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `rmse for incremental values`() {
            val values = List(10) { it }
            val rmse = values.rmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `rmse for square wave`() {
            val values = List(10) { if (it % 2 == 0) 0.0 else 1.0 }
            val inverted = List(10) { if (it % 2 == 0) 1.0 else 0.0 }

            val rmse = values.rmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0

            val rmse2 = values.rmse(inverted)
            log.trace { "rmse2=$rmse2" }
            rmse2.shouldBeNear(1.0, 1e-10)
        }

        @Test
        fun `rmse for sin wave`() {
            val sines = List(10) { sin(it.toDouble()) }
            val cosines = List(10) { cos(it.toDouble()) }
            val rmse = sines.rmse(cosines)
            log.trace { "rmse=$rmse" }
            val expected = sqrt(sines.zip(cosines).map { (sine, cosine) -> (sine - cosine).square() }.average())
            rmse.shouldBeNear(expected, 1e-10)
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `rmse for elements vs reversed elements`(@RandomValue size: Int) {
            val length = size.abs().coerceIn(10, 100)
            val values1 = List(length) { Random.nextInt(0, 100) }
            val values2 = values1.reversed()

            val rmse1 = values1.rmse(values2)
            val rmse2 = values2.rmse(values1)
            log.trace { "rmse=$rmse1, reversed rmse=$rmse2" }
            rmse1 shouldBeEqualTo rmse2
        }
    }

    @Nested
    inner class NormalizedRMSE {
        @Test
        fun `normalized rmse consumes actual sequence once`() {
            val expected = sequenceOf(2.0, 2.0).constrainOnce()
            val actual = sequenceOf(1.0, 3.0).constrainOnce()

            expected.normalizedRmse(actual).shouldBeNear(0.5, 1e-10)
        }

        @Test
        fun `normalized rmse for empty values`() {
            emptyList<Double>().normalizedRmse(emptyList()) shouldBeEqualTo 0.0
        }

        @Test
        fun `normalized rmse for identity values`() {
            val values = List(10) { 1.0 }
            val rmse = values.normalizedRmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `normalized rmse for incremental values`() {
            val values = List(10) { it }
            val rmse = values.normalizedRmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `normalized rmse for square wave`() {
            val values = List(10) { if (it % 2 == 0) 0.0 else 1.0 }
            val inverted = List(10) { if (it % 2 == 0) 1.0 else 0.0 }

            val rmse = values.normalizedRmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0

            val rmse2 = values.normalizedRmse(inverted)
            log.trace { "rmse2=$rmse2" }
            rmse2.shouldBeNear(1.0, 1e-10)
        }

        @Test
        fun `normalized rmse for sin wave`() {
            val sines = List(10) { sin(it.toDouble()) }
            val cosines = List(10) { cos(it.toDouble()) }
            val rmse = sines.normalizedRmse(cosines)
            log.trace { "rmse=$rmse" }
            val expectedRmse = sqrt(sines.zip(cosines).map { (sine, cosine) -> (sine - cosine).square() }.average())
            val expected = expectedRmse / (cosines.max() - cosines.min())
            rmse.shouldBeNear(expected, 1e-10)
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `normalized rmse for elements vs reversed elements`(@RandomValue size: Int) {
            val length = size.abs().coerceIn(10, 100)
            val values1 = List(length) { Random.nextInt(0, 100) }
            val values2 = values1.reversed()

            val rmse1 = values1.normalizedRmse(values2)
            val rmse2 = values2.normalizedRmse(values1)
            log.trace { "rmse=$rmse1, reversed rmse=$rmse2" }
            rmse1 shouldBeEqualTo rmse2
        }
    }
}
