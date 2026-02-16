package io.bluetape4k.math.commons


import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.math.MathConsts.BLOCK_SIZE
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class MovingSumTest {

    companion object: KLogging()

    private val doubleValues = List(10) { it.toDouble() }
    private val longValues = List(10) { it.toLong() }

    @Test
    fun `moving sum of double values with invalid block size`() {
        assertFailsWith<AssertionError> {
            doubleValues.movingSum(-1)
        }
        assertFailsWith<AssertionError> {
            doubleValues.movingSum(0)
        }
        assertFailsWith<AssertionError> {
            doubleValues.movingSum(1)
        }
    }

    @Test
    fun `moving sum of same double values`() {
        val blockSize = BLOCK_SIZE
        val sameValues = List(10) { 1.0 }

        val sums = sameValues.movingSum(blockSize)

        log.trace { "sums=$sums" }
        sums.size shouldBeEqualTo (sameValues.size - blockSize + 1)
        sums.distinct() shouldBeEqualTo listOf(blockSize.toDouble())
    }

    @Test
    fun `moving sum of double values`() {
        val sums = doubleValues.movingSum()
        log.trace { "sums=$sums" }
        sums.size shouldBeEqualTo (doubleValues.size - BLOCK_SIZE + 1)
        sums.sorted() shouldBeEqualTo sums
    }

    @Test
    fun `moving sum of long values with invalid block size`() {
        assertFailsWith<AssertionError> {
            longValues.movingSum(-1)
        }
        assertFailsWith<AssertionError> {
            longValues.movingSum(0)
        }
        assertFailsWith<AssertionError> {
            longValues.movingSum(1)
        }
    }

    @Test
    fun `moving sum of same long values`() {
        val blockSize = BLOCK_SIZE
        val sameValues = List(10) { 1.0 }

        val sums = sameValues.movingSum(blockSize)

        log.trace { "sums=$sums" }
        sums.size shouldBeEqualTo (sameValues.size - blockSize + 1)
        sums.distinct() shouldBeEqualTo listOf(blockSize.toDouble())
    }

    @Test
    fun `moving sum of long values`() {
        val sums = longValues.movingSum()
        log.trace { "sums=$sums" }
        sums.size shouldBeEqualTo (longValues.size - BLOCK_SIZE + 1)
        sums.sorted() shouldBeEqualTo sums
    }
}
