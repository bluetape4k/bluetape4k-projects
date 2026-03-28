package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LettuceBloomFilterTest: AbstractLettuceTest() {

    private lateinit var bloomFilter: LettuceBloomFilter

    @BeforeEach
    fun setup() {
        bloomFilter = LettuceBloomFilter(
            LettuceClients.connect(client, StringCodec.UTF8),
            "bf-${randomName()}",
            BloomFilterOptions(expectedInsertions = 1000L, falseProbability = 0.01),
        )
        bloomFilter.tryInit()
    }

    @AfterEach
    fun teardown() = bloomFilter.close()

    @Test
    fun `BloomFilterOptions - 잘못된 falseProbability 예외`() {
        assertThrows<IllegalArgumentException> { BloomFilterOptions(falseProbability = 0.0) }
        assertThrows<IllegalArgumentException> { BloomFilterOptions(falseProbability = 1.0) }
    }

    @Test
    fun `add - contains true`() {
        bloomFilter.add("hello")
        bloomFilter.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소는 false`() {
        bloomFilter.add("a")
        bloomFilter.contains("definitely-not-added-xyz").shouldBeFalse()
    }

    @Test
    fun `tryInit - 이미 초기화된 경우 false`() {
        bloomFilter.tryInit().shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() {
        val other = LettuceBloomFilter(
            LettuceClients.connect(client, StringCodec.UTF8),
            bloomFilter.filterName,
            BloomFilterOptions(expectedInsertions = 9999L, falseProbability = 0.5),
        )
        other.use {
            assertThrows<IllegalStateException> { it.tryInit() }
        }
    }

    @Test
    fun `다량 원소 추가 후 false positive rate 검증`() {
        val count = 500
        (1..count).forEach { bloomFilter.add("element-$it") }
        (1..count).forEach { bloomFilter.contains("element-$it").shouldBeTrue() }

        val falsePositives = (count + 1..count + 1000).count { bloomFilter.contains("element-$it") }
        assert(falsePositives < 50) { "false positive rate too high: $falsePositives/1000" }
    }
}
