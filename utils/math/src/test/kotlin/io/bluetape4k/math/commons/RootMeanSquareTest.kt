package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.math.cos
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
        fun `rms for empty values`() {
            emptyList<Double>().rms().apply { println("rms=$this") }
            doubleArrayOf().asSequence().rms() shouldBeEqualTo 0.0
        }

        @Test
        fun `rms for identity values`() {
            val values = fastList(10) { 1.0 }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms shouldBeEqualTo 1.4907119849998598
        }

        @Test
        fun `rms for incremental values`() {
            val values = fastList(10) { it }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms shouldBeEqualTo 3.1622776601683795
        }

        @Test
        fun `rms for square wave`() {
            val values = fastList(10) { if (it % 2 == 0) 0.0 else 1.0 }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms shouldBeEqualTo 1.0540925533894598
        }

        @Test
        fun `rms for sin wave`() {
            val values = fastList(10) { sin(it.toDouble()) }
            val rms = values.rms()
            log.trace { "rms=$rms" }
            rms shouldBeEqualTo 0.6591593100486879
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `rms for elements vs reversed elements`(@RandomValue size: Int) {
            val length = size.abs().coerceIn(10, 100)
            val values1 = fastList(length) { Random.nextInt(0, 100) }
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
        fun `rmse for empty values`() {
            emptyList<Double>().rmse(emptyList()) shouldBeEqualTo 0.0
        }

        @Test
        fun `rmse for identity values`() {
            val values = fastList(10) { 1.0 }
            val rmse = values.rmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `rmse for incremental values`() {
            val values = fastList(10) { it }
            val rmse = values.rmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `rmse for square wave`() {
            val values = fastList(10) { if (it % 2 == 0) 0.0 else 1.0 }
            val inverted = fastList(10) { if (it % 2 == 0) 1.0 else 0.0 }

            val rmse = values.rmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0

            val rmse2 = values.rmse(inverted)
            log.trace { "rmse2=$rmse2" }
            rmse2 shouldBeEqualTo 1.0540925533894598
        }

        @Test
        fun `rmse for sin wave`() {
            val sines = fastList(10) { sin(it.toDouble()) }
            val cosines = fastList(10) { cos(it.toDouble()) }
            val rmse = sines.rmse(cosines)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 1.0680428391683685
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `rmse for elements vs reversed elements`(@RandomValue size: Int) {
            val length = size.abs().coerceIn(10, 100)
            val values1 = fastList(length) { Random.nextInt(0, 100) }
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
        fun `normalized rmse for empty values`() {
            emptyList<Double>().normalizedRmse(emptyList()) shouldBeEqualTo 0.0
        }

        @Test
        fun `normalized rmse for identity values`() {
            val values = fastList(10) { 1.0 }
            val rmse = values.normalizedRmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `normalized rmse for incremental values`() {
            val values = fastList(10) { it }
            val rmse = values.normalizedRmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0
        }

        @Test
        fun `normalized rmse for square wave`() {
            val values = fastList(10) { if (it % 2 == 0) 0.0 else 1.0 }
            val inverted = fastList(10) { if (it % 2 == 0) 1.0 else 0.0 }

            val rmse = values.normalizedRmse(values)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.0

            val rmse2 = values.normalizedRmse(inverted)
            log.trace { "rmse2=$rmse2" }
            rmse2 shouldBeEqualTo 1.0540925533894598
        }

        @Test
        fun `normalized rmse for sin wave`() {
            val sines = fastList(10) { sin(it.toDouble()) }
            val cosines = fastList(10) { cos(it.toDouble()) }
            val rmse = sines.normalizedRmse(cosines)
            log.trace { "rmse=$rmse" }
            rmse shouldBeEqualTo 0.5367069679875341
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `normalized rmse for elements vs reversed elements`(@RandomValue size: Int) {
            val length = size.abs().coerceIn(10, 100)
            val values1 = fastList(length) { Random.nextInt(0, 100) }
            val values2 = values1.reversed()

            val rmse1 = values1.normalizedRmse(values2)
            val rmse2 = values2.normalizedRmse(values1)
            log.trace { "rmse=$rmse1, reversed rmse=$rmse2" }
            rmse1 shouldBeEqualTo rmse2
        }
    }
}
