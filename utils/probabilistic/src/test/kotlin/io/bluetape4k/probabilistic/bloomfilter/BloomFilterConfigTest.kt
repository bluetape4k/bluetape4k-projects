package io.bluetape4k.probabilistic.bloomfilter

import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BloomFilterConfigTest {

    @Test
    fun `expectedInsertions 는 양수여야 한다`() {
        assertThrows<IllegalArgumentException> {
            BloomFilterConfig(expectedInsertions = 0L)
        }
    }

    @Test
    fun `falsePositiveProbability 는 0과 1 사이여야 한다`() {
        assertThrows<IllegalArgumentException> {
            BloomFilterConfig(falsePositiveProbability = 0.0)
        }
        assertThrows<IllegalArgumentException> {
            BloomFilterConfig(falsePositiveProbability = 1.0)
        }
    }

    @Test
    fun `지원 가능한 bitSize 를 초과하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            BloomFilterConfig(expectedInsertions = Long.MAX_VALUE, falsePositiveProbability = 0.01)
        }
    }

    @Test
    fun `bitSize 와 hashFunctionCount 를 계산한다`() {
        val config = BloomFilterConfig(expectedInsertions = 10_000L, falsePositiveProbability = 0.01)

        config.bitSize shouldBeGreaterThan 0L
        config.hashFunctionCount shouldBeGreaterThan 0
        config.hashFunctionCount shouldBeInRange 1..20
    }
}
