package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.SuspendBloomFilter
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.assertPositiveNumber
import java.io.Serializable
import java.util.*

/**
 * Coroutines 방식의 Bloom Filter Bitset을 메모리에 저장하는 [SuspendBloomFilter] 구현체 입니다.
 *
 * ## 동작/계약
 * - 내부 [BitSet]을 mutate하며 suspend 함수는 현재 구현에서 비블로킹 메모리 연산입니다.
 * - [contains] 결과는 Bloom filter 특성에 따라 오탐 가능성이 있습니다.
 * - 해시 오프셋은 [Hasher.murmurHashOffset]으로 계산합니다.
 *
 * ```
 * val bloomFilter = InMemoryCoBloomFilter<String>()
 *
 * val values = List(ITEM_COUNT) { Fakers.fixedString(256) }
 *             .onEach { bloomFilter.add(it) }
 *
 * // 모든 값이 존재하는지 확인
 * values.all { bloomFilter.contains(it) }.shouldBeTrue()
 *
 * // 존재하지 않는 값 확인
 * bloomFilter.contains("not-exists").shouldBeFalse()
 * ```
 *
 * @param T 요소 타입
 * @property m BloomFilter 크기
 * @property k Hash 함수 개수
 */
class InMemorySuspendBloomFilter<T: Any>(
    override val m: Int,
    override val k: Int,
): SuspendBloomFilter<T> {

    companion object: KLogging() {
        private const val SEED32: Int = 89478583

        /**
         * [InMemorySuspendBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산합니다.
         * - 계산 결과가 0 이하이면 내부 assertion 예외가 발생할 수 있습니다.
         *
         * ```kotlin
         * val filter = InMemorySuspendBloomFilter<String>(maxNum = 1_000, errorRate = 1.0e-6)
         * // filter.m > 0 && filter.k > 0
         * ```
         *
         * @param maxNum 최대 요소 개수 (기본값: [DEFAULT_MAX_NUM])
         * @param errorRate 오류율 (기본값: [DEFAULT_ERROR_RATE])
         */
        @JvmStatic
        operator fun <T: Serializable> invoke(
            maxNum: Long = DEFAULT_MAX_NUM,
            errorRate: Double = DEFAULT_ERROR_RATE,
        ): InMemorySuspendBloomFilter<T> {
            val m = optimalM(maxNum, errorRate).assertPositiveNumber("m")
            val k = optimalK(maxNum, m).assertPositiveNumber("k")

            return InMemorySuspendBloomFilter<T>(m, k).apply {
                log.info { "Create InMemoryBloomFilter. m=$m, k=$k" }
            }
        }
    }

    private val bloom: BitSet = BitSet(m)

    override val isEmpty: Boolean
        get() = bloom.isEmpty

    override suspend fun count(): Long = m.toLong()

    override suspend fun clear() {
        bloom.clear()
    }

    override suspend fun contains(value: T): Boolean {
        val offsets = Hasher.murmurHashOffset(value, k, m)
        return offsets.all { bloom[it] }
    }

    override suspend fun add(value: T) {
        val offsets = Hasher.murmurHashOffset(value, k, m)
        offsets.forEach { bloom[it] = true }
    }
}
