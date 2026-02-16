package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InMemorySuspendBloomFilterEdgeCaseTest {
    companion object: KLoggingChannel() {
        private const val ITEM_COUNT = 100
    }

    private val bloomFilter = InMemorySuspendBloomFilter<String>()

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            bloomFilter.clear()
        }
    }

    @Test
    fun `빈 SuspendBloomFilter 검증`() =
        runSuspendDefault {
            bloomFilter.isEmpty.shouldBeTrue()
            bloomFilter.contains("any-value").shouldBeFalse()
        }

    @Test
    fun `suspend clear 후 검증`() =
        runSuspendDefault {
            val values = List(ITEM_COUNT) { Base58.randomString(32) }
            values.forEach { bloomFilter.add(it) }

            bloomFilter.isEmpty.shouldBeFalse()

            bloomFilter.clear()

            bloomFilter.isEmpty.shouldBeTrue()
        }

    @Test
    fun `동시에 여러 값 추가`() =
        runSuspendDefault {
            val values = List(ITEM_COUNT) { Base58.randomString(32) }

            // 동시에 추가
            val jobs =
                values.map { value ->
                    async { bloomFilter.add(value) }
                }
            jobs.awaitAll()

            // 모든 값이 존재하는지 확인
            values.all { bloomFilter.contains(it) }.shouldBeTrue()
        }

    @Test
    fun `동시에 여러 값 조회`() =
        runSuspendDefault {
            val values = List(ITEM_COUNT) { Base58.randomString(32) }
            values.forEach { bloomFilter.add(it) }

            // 동시에 조회
            val results =
                values
                    .map { value ->
                        async { bloomFilter.contains(value) }
                    }.awaitAll()

            results.all { it }.shouldBeTrue()
        }

    @Test
    fun `suspend count 메서드 검증`() =
        runSuspendDefault {
            val count = bloomFilter.count()
            count shouldBeEqualTo bloomFilter.m.toLong()
        }

    @Test
    fun `확률 계산은 suspend가 아님`() =
        runSuspendDefault {
            val n = 1000

            val zeroProb = bloomFilter.getBitZeroProbability(n)
            (zeroProb in 0.0..1.0).shouldBeTrue()

            val fpProb = bloomFilter.getFalsePositiveProbability(n)
            (fpProb in 0.0..1.0).shouldBeTrue()

            val bitsPerElement = bloomFilter.getBitsPerElement(n)
            (bitsPerElement > 0).shouldBeTrue()
        }
}
