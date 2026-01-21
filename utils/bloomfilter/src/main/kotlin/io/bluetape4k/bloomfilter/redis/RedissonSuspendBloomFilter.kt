package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.SuspendBloomFilter
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient

/**
 * Redisson 을 사용하는 Coroutines 방식의 Bloom Filter
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
        bitSet.setAsync(offsets, true).suspendAwait()
    }

    /**
     * 원소 포함 여부 검사
     *
     * ```
     * val valuee = Fakers.fixedString(256)
     * bloomFilter.add(value)
     *
     * bloomFilter.contains(value)  // true
     * bloomFilter.contains("not-exists") // false
     * ```
     */
    override suspend fun contains(value: T): Boolean {
        val offsets = getOffsets(value)
        val result = bitSet.getAsync(*offsets).suspendAwait()
        return result.all { it }
    }

    override suspend fun count(): Long {
        return bitSet.lengthAsync().suspendAwait()
    }

    override suspend fun clear() {
        bitSet.clearAsync().suspendAwait()
    }

    private fun getOffsets(value: T): LongArray =
        Hasher.murmurHashOffset(value, k, m).map { it.toLong() }.toLongArray()
}
