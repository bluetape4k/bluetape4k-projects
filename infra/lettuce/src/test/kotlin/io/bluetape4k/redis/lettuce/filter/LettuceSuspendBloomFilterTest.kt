package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LettuceSuspendBloomFilterTest : AbstractLettuceTest() {

    private lateinit var bf: LettuceSuspendBloomFilter

    @BeforeEach
    fun setup() = runTest {
        bf = LettuceSuspendBloomFilter(
            client.connect(StringCodec.UTF8),
            "sbf-${randomName()}",
            BloomFilterOptions(expectedInsertions = 1000L, falseProbability = 0.01),
        )
        bf.tryInit()
    }

    @AfterEach
    fun teardown() = bf.close()

    @Test
    fun `add - contains true`() = runTest {
        bf.add("hello")
        bf.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소 false`() = runTest {
        bf.contains("not-added-xyz").shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() = runTest {
        val bf2 = LettuceSuspendBloomFilter(
            client.connect(StringCodec.UTF8),
            bf.filterName,
            BloomFilterOptions(expectedInsertions = 9999L, falseProbability = 0.5),
        )
        bf2.use {
            assertThrows<IllegalStateException> { it.tryInit() }
        }
    }
}
