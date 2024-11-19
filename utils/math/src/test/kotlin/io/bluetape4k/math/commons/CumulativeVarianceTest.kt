package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class CumulativeVarianceTest {

    companion object: KLogging()

    @Test
    fun `cumulative variance for empty sequence`() {
        assertFailsWith<NoSuchElementException> {
            emptySequence<Double>().cumulativeVariance().toList()
        }
    }

    @Test
    fun `cumulative variance for zero sequence`() {
        val zeros = sequence { repeat(100) { yield(0.0) } }

        val cv = zeros.cumulativeVariance()
        cv.all { it == 0.0 }.shouldBeTrue()
    }

    @Test
    fun `cumulative variance for same values`() {
        val ones = sequence { repeat(100) { yield(42.0) } }

        val cv = ones.cumulativeVariance().toList()
        cv.all { it == 0.0 }.shouldBeTrue()
    }

    @Test
    fun `cumulative variance for incremental values`() {
        val incs = sequence { repeat(100) { yield(it.toDouble()) } }

        val cv = incs.cumulativeVariance().toList()
        log.trace { "cv=$cv" }
        cv[0] shouldBeEqualTo 0.5
        cv[1] shouldBeEqualTo 1.0
        cv.sorted() shouldBeEqualTo cv
    }

    @Test
    fun `cumulative variance for decremental values`() {
        val decs = sequence { repeat(100) { yield(100.0 - it.toDouble()) } }

        val cv = decs.cumulativeVariance().toList()
        log.trace { "cv=$cv" }
        cv[0] shouldBeEqualTo 0.5
        cv[1] shouldBeEqualTo 1.0
        cv.sorted() shouldBeEqualTo cv
    }

    @Test
    fun `cumulative variance for random values`() {
        val values = sequence { repeat(100) { yield(Random.nextDouble(-10.0, 10.0)) } }
        val cv = values.cumulativeVariance().toList()
        log.trace { "cv=$cv" }
        cv.all { it >= 0 }.shouldBeTrue()
    }
}
