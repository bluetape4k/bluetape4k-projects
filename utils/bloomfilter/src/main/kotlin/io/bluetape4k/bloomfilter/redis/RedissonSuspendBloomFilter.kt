package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.SuspendBloomFilter
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient

/**
 * Redisson 을 사용하는 Coroutines 방식의 Bloom Filter
 *
 * ## 동작/계약
 * - 내부적으로 Redisson BitSet 비동기 API를 사용하며 Redis에 상태를 저장합니다.
 * - [bloomName]이 blank면 생성 시 [IllegalArgumentException]이 발생합니다.
 * - [contains] `true`는 오탐 가능성이 있고 `false`는 미포함이 확정입니다.
 *
 * ```
 * val redisson = Redisson.create()
 * val bloomFilter = RedissonCoBloomFilter(redisson, "bloom-filter")
 *
 * val items = List(ITEM_SIZE) { Fakers.fixedString(16) }.distinct()
 * items.forEach {
 *     bloomFilter.add(it)
 * }
 *
 * // 기존 Item이 존재하는지 검증
 * items.all { bloomFilter.contains(it) }.shouldBeTrue()
 *
 * // 기존 Item이 아닌 값이 존재하지 않는지 검증
 * bloomFilter.contains("not-exists").shouldBeFalse()
 * ```
 *
 * @property redisson Redisson Client
 * @property bloomName Bloom Filter 이름
 * @property m Bloom Filter 크기
 * @property k Hash 함수 개수
 */
class RedissonSuspendBloomFilter<T: Any> private constructor(
    private val redisson: RedissonClient,
    private val bloomName: String,
    override val m: Int,
    override val k: Int,
): SuspendBloomFilter<T> {

    companion object: KLoggingChannel() {
        /**
         * [RedissonSuspendBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [bloomName]이 blank면 [IllegalArgumentException]을 던집니다.
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산해 생성합니다.
         *
         * ```kotlin
         * val filter = RedissonSuspendBloomFilter<String>(redisson, "user-email")
         * // filter.m > 0 && filter.k > 0
         * ```
         *
         * @param redisson Redisson Client
         * @param bloomName Bloom Filter 이름
         * @param maxNum 최대 요소 개수 (기본값: [DEFAULT_MAX_NUM])
         * @param errorRate 오류율 (기본값: [DEFAULT_ERROR_RATE])
         */
        @JvmStatic
        operator fun <T: Any> invoke(
            redisson: RedissonClient,
            bloomName: String,
            maxNum: Long = DEFAULT_MAX_NUM,
            errorRate: Double = DEFAULT_ERROR_RATE,
        ): RedissonSuspendBloomFilter<T> {
            bloomName.requireNotBlank("bloomName")

            val m = optimalM(maxNum, errorRate)
            val k = optimalK(maxNum, m)

            return RedissonSuspendBloomFilter<T>(redisson, bloomName, m, k).apply {
                log.info { "Create RedissonSuspendBloomFilter, name=$bloomName, m=$m, k=$k" }
            }
        }
    }

    private val bitSet by lazy { redisson.getBitSet(bloomName) }

    override val isEmpty: Boolean get() = !bitSet.isExists

    override suspend fun add(value: T) {
        val offsets = getOffsets(value)
        bitSet.setAsync(offsets, true).awaitSuspending()
    }

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - 비동기 비트 조회 결과가 모두 `true`이면 `true`를 반환합니다.
     * - Redis I/O 예외는 suspend 호출자에게 전파됩니다.
     *
     * ```kotlin
     * bloomFilter.add("alpha")
     * // bloomFilter.contains("alpha") == true
     * ```
     */
    override suspend fun contains(value: T): Boolean {
        val offsets = getOffsets(value)
        val result = bitSet.getAsync(*offsets).awaitSuspending()
        return result.all { it }
    }

    override suspend fun count(): Long {
        return bitSet.lengthAsync().awaitSuspending()
    }

    override suspend fun clear() {
        bitSet.clearAsync().awaitSuspending()
    }

    private fun getOffsets(value: T): LongArray =
        Hasher.murmurHashOffset(value, k, m).map { it.toLong() }.toLongArray()
}
