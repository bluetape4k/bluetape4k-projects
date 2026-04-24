package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LettuceSuspendBloomFilterTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var bloomFilter: LettuceSuspendBloomFilter

    @BeforeEach
    fun setup() = runTest {
        bloomFilter = LettuceSuspendBloomFilter(
            connection,
            "sbf-${randomName()}",
            BloomFilterOptions(expectedInsertions = 1000L, falseProbability = 0.01),
        )
        bloomFilter.tryInit()
    }

    @Test
    fun `add - contains true`() = runTest {
        bloomFilter.add("hello")
        bloomFilter.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소 false`() = runTest {
        bloomFilter.contains("not-added-xyz").shouldBeFalse()
    }

    @Test
    fun `tryInit - 이미 초기화된 경우 false`() = runTest {
        bloomFilter.tryInit().shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() = runTest {
        val other = LettuceSuspendBloomFilter(
            connection,
            bloomFilter.filterName,
            BloomFilterOptions(expectedInsertions = 9999L, falseProbability = 0.5),
        )
        assertThrows<IllegalStateException> { other.tryInit() }
    }
}
