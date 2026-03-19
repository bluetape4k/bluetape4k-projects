package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.bloomfilter.BloomFilter
import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.assertPositiveNumber
import java.io.Serializable
import java.util.*

/**
 * 메모리에 BloomFilter의 BitSet 을 저장하는 [BloomFilter] 구현체 입니다.
 *
 * ## 동작/계약
 * - 내부 [BitSet]을 mutate하며 add/clear 연산을 수행합니다.
 * - [contains]의 `true`는 오탐 가능성이 있으며 `false`는 미포함이 확정입니다.
 * - 해시 오프셋 계산은 [Hasher.murmurHashOffset]을 사용합니다.
 *
 * ```
 * val bloomFilter = InMemoryBloomFilter<String>()
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
open class InMemoryBloomFilter<T: Serializable> private constructor(
    override val m: Int,
    override val k: Int,
): BloomFilter<T> {

    companion object: KLogging() {
        protected const val SEED32: Int = 89478583

        /**
         * [InMemoryBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산합니다.
         * - 계산 결과가 0 이하이면 내부 assertion 예외가 발생할 수 있습니다.
         *
         * ```kotlin
         * val filter = InMemoryBloomFilter<String>(maxNum = 1_000, errorRate = 1.0e-6)
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
        ): InMemoryBloomFilter<T> {
            val m = optimalM(maxNum, errorRate).assertPositiveNumber("m")
            val k = optimalK(maxNum, m).assertPositiveNumber("k")

            return InMemoryBloomFilter<T>(m, k).apply {
                log.info { "Create InMemoryBloomFilter. m=$m, k=$k" }
            }
        }
    }

    protected val bloom: BitSet = BitSet(m)

    override val isEmpty: Boolean get() = bloom.isEmpty

    override fun count(): Long = m.toLong()

    override fun add(value: T) {
        val offsets = Hasher.murmurHashOffset(value, k, m)
        offsets.forEach { setBit(it, true) }
    }

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - 계산된 해시 오프셋의 모든 비트가 `true`일 때만 `true`를 반환합니다.
     * - 오탐 가능성이 있으므로 결과 `true`는 확정 포함이 아닙니다.
     *
     * ```kotlin
     * bloomFilter.add("alpha")
     * // bloomFilter.contains("alpha") == true
     * ```
     */
    override fun contains(value: T): Boolean {
        val offsets = Hasher.murmurHashOffset(value, k, m)
        return offsets.all { getBit(it) }
    }

    override fun clear() {
        bloom.clear()
    }

    protected fun getBit(index: Int): Boolean = bloom[index]

    protected fun setBit(index: Int, value: Boolean = true) {
        bloom[index] = value
    }
}
