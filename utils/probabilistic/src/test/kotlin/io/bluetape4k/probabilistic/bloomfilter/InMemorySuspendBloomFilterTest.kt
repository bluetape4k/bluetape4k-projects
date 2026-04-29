package io.bluetape4k.probabilistic.bloomfilter

import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class InMemorySuspendBloomFilterTest {

    @Test
    fun `suspend BloomFilter 는 메모리 연산을 위임한다`() = runTest {
        val filter = suspendBloomFilter<String>(expectedInsertions = 1_000L, fpp = 0.01)

        filter.put("alpha").shouldBeTrue()

        filter.mightContain("alpha").shouldBeTrue()
        filter.mightContain("missing").shouldBeFalse()
        filter.approximateElementCount() shouldBeGreaterThan 0L
    }

    @Test
    fun `suspend clear 는 필터를 초기화한다`() = runTest {
        val filter = suspendBloomFilter<String>(expectedInsertions = 1_000L, fpp = 0.01)
        filter.put("alpha")

        filter.clear()

        filter.isEmpty.shouldBeTrue()
        filter.mightContain("alpha").shouldBeFalse()
    }
}
