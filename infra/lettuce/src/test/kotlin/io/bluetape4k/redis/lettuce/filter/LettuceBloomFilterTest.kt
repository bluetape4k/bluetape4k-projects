package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LettuceBloomFilterTest : AbstractLettuceTest() {

    private lateinit var bf: LettuceBloomFilter

    @BeforeEach
    fun setup() {
        bf = LettuceBloomFilter(
            client.connect(StringCodec.UTF8),
            "bf-${randomName()}",
            BloomFilterOptions(expectedInsertions = 1000L, falseProbability = 0.01),
        )
        bf.tryInit()
    }

    @AfterEach
    fun teardown() = bf.close()

    @Test
    fun `BloomFilterOptions - 잘못된 falseProbability 예외`() {
        assertThrows<IllegalArgumentException> { BloomFilterOptions(falseProbability = 0.0) }
        assertThrows<IllegalArgumentException> { BloomFilterOptions(falseProbability = 1.0) }
    }

    @Test
    fun `add - contains true`() {
        bf.add("hello")
        bf.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소는 false`() {
        bf.add("a")
        bf.contains("definitely-not-added-xyz").shouldBeFalse()
    }

    @Test
    fun `tryInit - 이미 초기화된 경우 false`() {
        bf.tryInit().shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() {
        val bf2 = LettuceBloomFilter(
            client.connect(StringCodec.UTF8),
            bf.filterName,  // 같은 key
            BloomFilterOptions(expectedInsertions = 9999L, falseProbability = 0.5),
        )
        bf2.use {
            assertThrows<IllegalStateException> { it.tryInit() }
        }
    }

    @Test
    fun `다량 원소 추가 후 false positive rate 검증`() {
        val n = 500
        (1..n).forEach { bf.add("element-$it") }

        // 추가한 원소는 반드시 true
        (1..n).forEach { bf.contains("element-$it").shouldBeTrue() }

        // 오탐 검사: 1000개 미추가 원소 중 오탐 비율 < 5%
        val falsePositives = (n + 1..n + 1000).count { bf.contains("element-$it") }
        assert(falsePositives < 50) { "false positive rate too high: $falsePositives/1000" }
    }
}
