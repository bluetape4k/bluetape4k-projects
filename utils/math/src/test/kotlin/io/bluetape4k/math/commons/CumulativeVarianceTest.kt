package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class CumulativeVarianceTest {

    companion object: KLogging() {
        private const val SAMPLE_SIZE = 100
    }

    @Test
    fun `cumulative variance for empty sequence`() {
        assertFailsWith<NoSuchElementException> {
            emptySequence<Double>().cumulativeVariance()
        }
    }

    @Test
    fun `cumulative variance for zero sequence`() {
        val zeros = List(SAMPLE_SIZE) { 0.0 }

        val cv = zeros.cumulativeVariance()
        cv.all { it == 0.0 }.shouldBeTrue()
    }

    @Test
    fun `cumulative variance for same values`() {
        val ones = List(SAMPLE_SIZE) { 42.0 }

        val cv = ones.cumulativeVariance()
        cv.all { it == 0.0 }.shouldBeTrue()
    }

    @Test
    fun `cumulative variance for incremental values`() {
        val incs = List(SAMPLE_SIZE) { it.toDouble() }

        val cv = incs.cumulativeVariance()
        log.trace { "cv=$cv" }
        cv[0] shouldBeEqualTo 0.5
        cv[1] shouldBeEqualTo 1.0
        cv.sorted() shouldBeEqualTo cv
    }

    @Test
    fun `cumulative variance for decremental values`() {
        val decs = List(SAMPLE_SIZE) { SAMPLE_SIZE - it.toDouble() }

        val cv = decs.cumulativeVariance()
        log.trace { "cv=$cv" }
        cv[0] shouldBeEqualTo 0.5
        cv[1] shouldBeEqualTo 1.0
        cv.sorted() shouldBeEqualTo cv
    }

    @Test
    fun `cumulative variance for random values`() {
        val values = List(SAMPLE_SIZE) { Random.nextDouble(-10.0, 10.0) }
        val cv = values.cumulativeVariance()
        log.trace { "cv=$cv" }
        cv.all { it >= 0 }.shouldBeTrue()
    }
}
