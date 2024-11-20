package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.BloomFilter
import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.redisson.api.RedissonClient

/**
 * Redisson 을 사용하는 Bloom Filter
 *
 * ```
 * val redisson = Redisson.create()
 * val bloomFilter = RedissonBloomFilter(redisson, "bloom-filter")
 *
 * val items = List(ITEM_SIZE) { Fakers.fixedString(16) }.distinct()
 * items.forEach {
 *      bloomFilter.add(it)
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
class RedissonBloomFilter<T: Any> private constructor(
    private val redisson: RedissonClient,
    private val bloomName: String,
    override val m: Int,
    override val k: Int,
): BloomFilter<T> {

    companion object: KLogging() {
        /**
         * [RedissonBloomFilter] 를 생성합니다.
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
        ): RedissonBloomFilter<T> {
            val m = optimalM(maxNum, errorRate)
            val k = optimalK(maxNum, m)

            return RedissonBloomFilter<T>(redisson, bloomName, m, k).apply {
                log.info { "Create RedissonBloomFilter, name=$bloomName, m=$m, k=$k" }
            }
        }
    }

    override val isEmpty: Boolean get() = !redisson.getBitSet(bloomName).isExists

    override fun add(value: T) {
        val offsets = Hasher.murmurHashOffset(value, k, m)

        val batch = redisson.createBatch()
        val bloomAsync = batch.getBitSet(bloomName)

        offsets.forEach { bloomAsync.setAsync(it.toLong()) }
        batch.execute()
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
    override fun contains(value: T): Boolean {
        val offsets = Hasher.murmurHashOffset(value, k, m)
        val batch = redisson.createBatch()
        val bloomAsync = batch.getBitSet(bloomName)

        offsets.forEach { bloomAsync.getAsync(it.toLong()) }
        val result = batch.execute()

        return result.responses.all { it as Boolean }
    }

    override fun count(): Long {
        return redisson.getBitSet(bloomName).length()
    }

    override fun clear() {
        redisson.getBitSet(bloomName).clear()
    }
}
