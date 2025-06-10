package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.CoBloomFilter
import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.coroutines.support.coAwait
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
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
@Deprecated("Use RedissonSuspendBloomFilter instead", ReplaceWith("RedissonSuspendBloomFilter"))
class RedissonCoBloomFilter<T: Any> private constructor(
    private val redisson: RedissonClient,
    private val bloomName: String,
    override val m: Int,
    override val k: Int,
): CoBloomFilter<T> {

    companion object: KLoggingChannel() {
        /**
         * [RedissonCoBloomFilter] 를 생성합니다.
         *
         * @param redisson Redisson Client
         * @param bloomName Bloom Filter 이름
         * @param maxNum 최대 요소 개수 (기본값: [DEFAULT_MAX_NUM])
         * @param errorRate 오류율 (기본값: [DEFAULT_ERROR_RATE])
         */
        @Deprecated("Use RedissonSuspendBloomFilter instead", ReplaceWith("RedissonSuspendBloomFilter"))
        @JvmStatic
        operator fun <T: Any> invoke(
            redisson: RedissonClient,
            bloomName: String,
            maxNum: Long = DEFAULT_MAX_NUM,
            errorRate: Double = DEFAULT_ERROR_RATE,
        ): RedissonCoBloomFilter<T> {
            val m = optimalM(maxNum, errorRate)
            val k = optimalK(maxNum, m)

            return RedissonCoBloomFilter<T>(redisson, bloomName, m, k).apply {
                log.info { "Create RedissonBloomFilter, name=$bloomName, m=$m, k=$k" }
            }
        }
    }

    override val isEmpty: Boolean get() = !redisson.getBitSet(bloomName).isExists

    override suspend fun add(value: T) {
        val offsets = Hasher.murmurHashOffset(value, k, m)

        val batch = redisson.createBatch()
        val bloomAsync = batch.getBitSet(bloomName)

        offsets.forEach { bloomAsync.setAsync(it.toLong()) }
        batch.executeAsync().coAwait()
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
        val offsets = Hasher.murmurHashOffset(value, k, m)
        val batch = redisson.createBatch()

        val bloomAsync = batch.getBitSet(bloomName)
        offsets.forEach { bloomAsync.getAsync(it.toLong()) }

        val result = batch.executeAsync().coAwait()
        return result.responses.all { it as Boolean }
    }

    override suspend fun count(): Long {
        return redisson.getBitSet(bloomName).lengthAsync().coAwait()
    }

    override suspend fun clear() {
        redisson.getBitSet(bloomName).clearAsync().coAwait()
    }
}
