package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class MovingAverageTest {

    companion object: KLogging()

    @Test
    fun `standard moving average with small blockSize`() {
        assertFailsWith<AssertionError> {
            sequenceOf(0.0, 1.0).standardMovingAverage(0)
        }
        assertFailsWith<AssertionError> {
            sequenceOf(0.0, 1.0).standardMovingAverage(1)
        }
        assertFailsWith<AssertionError> {
            listOf(0.0, 1.0).standardMovingAverage(1)
        }
    }

    @Test
    fun `standard moving average with same value`() {
        val onlyOne = List(10) { 1.0 }
        val avgs = onlyOne.asSequence().standardMovingAverage().toList()
        log.trace { "mg size=${avgs.size}, $avgs" }
        avgs.distinct() shouldBeEqualTo listOf(1.0)
    }

    @Test
    fun `standard moving average with values`() {
        val total = 100
        val blockSize = 4

        val onlyOne = List(total) { it.toDouble() }
        val avgs = onlyOne.asSequence().standardMovingAverage(blockSize).toList()
        log.trace { "mg size=${avgs.size}, $avgs" }

        avgs.size shouldBeEqualTo (total - blockSize + 1)
        avgs.sorted() shouldBeEqualTo avgs
    }

    @Test
    fun `exponential moving average with small blockSize`() {
        assertFailsWith<AssertionError> {
            sequenceOf(0.0, 1.0).exponentialMovingAverage(0)
        }
        assertFailsWith<AssertionError> {
            sequenceOf(0.0, 1.0).exponentialMovingAverage(1)
        }
        assertFailsWith<AssertionError> {
            listOf(0.0, 1.0).exponentialMovingAverage(1)
        }
    }

    @Test
    fun `exponential moving average with same value`() {
        val onlyOne = List(10) { 1.0 }
        val avgs = onlyOne.exponentialMovingAverage()
        log.trace { "avgs=$avgs" }
        avgs.sorted() shouldBeEqualTo avgs
    }

    @Test
    fun `exponential moving average with values`() {
        val total = 100
        val blockSize = 4

        val onlyOne = List(total) { it.toDouble() }
        val avgs = onlyOne.exponentialMovingAverage(blockSize)
        log.trace { "mg size=${avgs.size}, $avgs" }

        avgs.size shouldBeEqualTo (total - blockSize + 1)
        avgs.sorted() shouldBeEqualTo avgs
    }

    @Test
    fun `cumulative moving average with same value`() {
        val onlyOne = List(10) { 1.0 }
        val avgs = onlyOne.cumulativeMovingAverage()
        log.trace { "mg size=${avgs.size}, $avgs" }
        avgs.distinct() shouldBeEqualTo listOf(1.0)
    }

    @Test
    fun `cumulative moving average with values`() {
        val total = 100

        val onlyOne = List(total) { it.toDouble() }
        val avgs = onlyOne.cumulativeMovingAverage()
        log.trace { "mg size=${avgs.size}, $avgs" }

        avgs.size shouldBeEqualTo total
        avgs.sorted() shouldBeEqualTo avgs
    }

    @Test
    fun `weighted moving average with small blockSize`() {
        assertFailsWith<AssertionError> {
            sequenceOf(0.0, 1.0).weightedMovingAverage(0) { it.toDouble() }
        }
        assertFailsWith<AssertionError> {
            sequenceOf(0.0, 1.0).weightedMovingAverage(1) { it.toDouble() }
        }
        assertFailsWith<AssertionError> {
            listOf(0.0, 1.0).weightedMovingAverage(1) { it.toDouble() }
        }
    }

    @Test
    fun `weighted moving average with same value`() {
        val onlyOne = List(10) { 1.0 }
        val avgs = onlyOne.weightedMovingAverage { 1.0 }
        log.trace { "mg size=${avgs.size}, $avgs" }
        avgs.distinct() shouldBeEqualTo listOf(1.0)
    }

    @Test
    fun `weighted moving average with values`() {
        val total = 100
        val blockSize = 4

        val onlyOne = List(total) { it.toDouble() }
        val avgs = onlyOne.weightedMovingAverage(blockSize) { 1.0 }
        log.trace { "mg size=${avgs.size}, $avgs" }

        avgs.size shouldBeEqualTo (total - blockSize + 1)
        avgs.sorted() shouldBeEqualTo avgs
    }
}
