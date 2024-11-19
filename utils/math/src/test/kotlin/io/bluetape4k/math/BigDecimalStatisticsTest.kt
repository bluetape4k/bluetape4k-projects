package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalStatisticsTest {

    companion object: KLogging()

    private val valueVector = sequenceOf(1.0, 3.0, 5.0, 11.0).map { it.toBigDecimal() }
    private val groups = sequenceOf("A", "A", "B", "B")

    private val grouped: Sequence<Pair<String, BigDecimal>> = groups.zip(valueVector)

    @Test
    fun `sum by for BigDecimal array`() {
        val expected = mapOf(
            "A" to 4.0,
            "B" to 16.0
        )

        grouped.sumBy() shouldBeEqualTo expected
        grouped.sumBy({ it.first }, { it.second }) shouldBeEqualTo expected
    }

    @Test
    fun `average by for BigDecimal array`() {
        val expected = mapOf(
            "A" to 2.0,
            "B" to 8.0
        )

        grouped.averageBy() shouldBeEqualTo expected
        grouped.averageBy({ it.first }, { it.second }) shouldBeEqualTo expected
    }
}
