package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DoubleStatisticsTest {

    companion object: KLogging()

    private val valueVector = sequenceOf(0.0, 1.0, 3.0, 5.0, 11.0)
    private val groups = sequenceOf("A", "B", "B", "C", "C")
    private val grouped: Sequence<Pair<String, Double>> = groups.zip(valueVector)

    @Test
    fun `calc median`() {
        sequenceOf(1.0, 3.0, 5.0).median() shouldBeEqualTo 3.0
        sequenceOf(1.0, 3.0, 4.0, 5.0).median() shouldBeEqualTo 3.5
    }

    @Test
    fun `calc variance`() {
        doubleArrayOf(2.0, 3.0, 4.0).variance() shouldBeEqualTo 1.0
        doubleArrayOf(1.0, 4.0, 6.0, 10.0).variance() shouldBeEqualTo 14.25
    }

    @Test
    fun `sum by for BigDecimal array`() {
        val expected = mapOf("A" to 0.0, "B" to 4.0, "C" to 16.0)

        grouped.sumBy() shouldBeEqualTo expected
        grouped.sumBy({ it.first }, { it.second }) shouldBeEqualTo expected
    }

    @Test
    fun `average by for BigDecimal array`() {
        val expected = mapOf("A" to 0.0, "B" to 2.0, "C" to 8.0)

        grouped.averageBy() shouldBeEqualTo expected
        grouped.averageBy({ it.first }, { it.second }) shouldBeEqualTo expected
    }
}
