package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ComparableStatisticsTest {

    companion object: KLogging()

    private val doubleVector = sequenceOf(1.0, 3.0, 5.0, 11.0)
    private val groups = sequenceOf("A", "A", "B", "B")
    private val grouped = groups.zip(doubleVector)

    @Test
    fun `minBy for double array`() {
        val expected = mapOf("A" to 1.0, "B" to 5.0)

        grouped.minBy() shouldBeEqualTo expected

        val min = grouped.minBy(
            keySelector = { it.first },
            valueSelector = { it.second }
        )
        min shouldBeEqualTo expected
    }

    @Test
    fun `maxBy for double array`() {
        val expected = mapOf("A" to 3.0, "B" to 11.0)

        grouped.maxBy() shouldBeEqualTo expected

        val max = grouped.maxBy(
            keySelector = { it.first },
            valueSelector = { it.second }
        )
        max shouldBeEqualTo expected
    }
}
