package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.MutableBloomFilter
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.assertPositiveNumber
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.setAll
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * In-Memory Mutable Bloom Filter
 *
 * ## 동작/계약
 * - 카운팅 버킷(LongArray)을 사용해 add/remove를 지원합니다.
 * - [add], [contains], [remove], [approximateCount]는 [value] blank 입력 시 예외를 던집니다.
 * - 동시성 경합 완화를 위해 hash slot별 [AtomicBoolean]/락을 사용합니다.
 *
 * ```
 * val bloomFilter = InMemoryMutableBloomFilter()
 *
 * val values = List(ITEM_COUNT) { Fakers.fixedString(256) }
 *            .onEach { bloomFilter.add(it) }
 *
 * // 모든 값이 존재하는지 확인
 * values.all { bloomFilter.contains(it) }.shouldBeTrue()
 *
 * // 존재하지 않는 값 확인
 * bloomFilter.contains("not-exists").shouldBeFalse()
 *
 *
 * val expectedItem = Fakers.fixedString(16)
 * // 하나의 요소 등록 작업
 * bloomFilter.add(expectedItem)
 * bloomFilter.contains(expectedItem).shouldBeTrue()
 *
 * // 기존 요소 제거
 * bloomFilter.remove(expectedItem)
 * bloomFilter.contains(expectedItem).shouldBeFalse()
 * ```
 *
 * @property m BloomFilter 크기
 * @property k Hash 함수 개수
 */
class InMemoryMutableBloomFilter private constructor(
    override val m: Int,
    override val k: Int,
): MutableBloomFilter<String> {

    companion object: KLogging() {
        private const val SEED32: Int = 89478583
        private const val HASH_LOCK_SIZE = 16
        private const val BUCKET_MAX_VALUE = HASH_LOCK_SIZE - 1

        /**
         * [InMemoryMutableBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산합니다.
         * - 계산 결과가 0 이하이면 내부 assertion 예외가 발생합니다.
         *
         * ```kotlin
         * val filter = InMemoryMutableBloomFilter(maxNum = 10_000, errorRate = 1.0e-6)
         * // filter.m > 0 && filter.k > 0
         * ```
         *
         * @param maxNum 최대 요소 개수 (기본값: [DEFAULT_MAX_NUM])
         * @param errorRate 오류율 (기본값: [DEFAULT_ERROR_RATE])
         */
        @JvmStatic
        operator fun invoke(
            maxNum: Long = DEFAULT_MAX_NUM,
            errorRate: Double = DEFAULT_ERROR_RATE,
        ): InMemoryMutableBloomFilter {
            val m = optimalM(maxNum, errorRate).assertPositiveNumber("m")
            val k = optimalK(maxNum, m).assertPositiveNumber("k")

            return InMemoryMutableBloomFilter(m, k)
        }

        private fun buckets2words(m: Int): Int = ((m - 1) ushr 4) + 1

        private fun long2bytes(num: Long): ByteArray =
            ByteArray(8) { (num ushr (56 - (it * 8))).toByte() }
    }

    private val buckets: LongArray = LongArray(buckets2words(m))
    private val hashLocks: Array<ReentrantLock> = Array(HASH_LOCK_SIZE) { ReentrantLock() }
    private val hashBooleans = Array(HASH_LOCK_SIZE) { AtomicBoolean(false) }
    private val lock = ReentrantLock()

    override val isEmpty: Boolean
        get() = buckets.all { it == 0L }

    override fun count(): Long = m.toLong()

    override fun add(value: String) {
        value.requireNotBlank("value")

        val hashes = Hasher.murmurHashOffset(value, k, m)
        var reuse = false

        repeat(k) { i ->
            val hash = hashes[i]
            val (wordNum, bucketShift, bucketMask) = calcBucketInfo(hash)
            var isExecuted = false

            while (!isExecuted && (reuse || (!reuse && getHashBoolean(hash).compareAndSet(false, true)))) {
                val bucketValue = (buckets[wordNum] and bucketMask) ushr bucketShift
                if (bucketValue < BUCKET_MAX_VALUE) {
                    buckets[wordNum] = (buckets[wordNum] and bucketMask.inv()) or ((bucketValue + 1) shl bucketShift)
                }

                if (i + 1 >= k || hashes[i + 1] != hashes[i]) {
                    reuse = false
                    getHashBoolean(hash).compareAndSet(true, false)
                } else {
                    reuse = true
                }
                isExecuted = true
            }
        }
    }

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - [value]가 blank면 [IllegalArgumentException]이 발생합니다.
     * - 모든 해시 버킷 값이 0이 아니면 `true`를 반환합니다.
     *
     * ```kotlin
     * bloomFilter.add("alpha")
     * // bloomFilter.contains("alpha") == true
     * ```
     */
    override fun contains(value: String): Boolean {
        value.requireNotBlank("value")
        val hashes = Hasher.murmurHashOffset(value, k, m)

        repeat(k) { i ->
            val hash = hashes[i]
            val (wordNum, _, bucketMask) = calcBucketInfo(hash)
            if ((buckets[wordNum] and bucketMask) == 0L) {
                return false
            }
        }
        return true
    }

    override fun remove(value: String) {
        value.requireNotBlank("value")
        if (!contains(value)) {
            return
        }

        val hashes = Hasher.murmurHashOffset(value, k, m)
        var reuse = false

        repeat(k) { i ->
            val hash = hashes[i]
            val (wordNum, bucketShift, bucketMask) = calcBucketInfo(hash)
            var isExecuted = false

            while (!isExecuted && (reuse || (!reuse && getHashBoolean(hash).compareAndSet(false, true)))) {
                val bucketValue = (buckets[wordNum] and bucketMask) ushr bucketShift
                if (bucketValue in 1 until BUCKET_MAX_VALUE) {
                    buckets[wordNum] = (buckets[wordNum] and bucketMask.inv()) or ((bucketValue - 1) shl bucketShift)
                    hashBooleans[BUCKET_MAX_VALUE].compareAndSet(true, false)
                }

                if (i + 1 >= k || hashes[i + 1] != hashes[i]) {
                    reuse = false
                    getHashBoolean(hash).compareAndSet(true, false)
                } else {
                    reuse = true
                }
                isExecuted = true
            }
        }
    }

    /**
     * 하나의 key가 얼마나 추가되었는지 추정한다
     *
     * @param value
     * @return
     */
    override fun approximateCount(value: String): Int {
        value.requireNotBlank("value")

        var res = Int.MAX_VALUE
        val hashes = Hasher.murmurHashOffset(value, k, m)

        repeat(k) { i ->
            val hash = hashes[i]
            val (wordNum, bucketShift, bucketMask) = calcBucketInfo(hash)
            val bucketValue = (buckets[wordNum] and bucketMask) ushr bucketShift

            if (bucketValue < res.toLong())
                res = bucketValue.toInt()
        }
        return if (res != Int.MAX_VALUE) res else 0
    }


    override fun clear() {
        lock.withLock {
            buckets.setAll { 0L }
            hashLocks.filter { it.isLocked }.forEach { it.unlock() }
            hashBooleans.forEach { it.set(false) }
        }
    }

    override fun toString(): String = buildString {
        repeat(m) { i ->
            if (i > 0) {
                append(" ")
            }
            val (wordNum, bucketShift, bucketMask) = calcBucketInfo(i)
            val bucketValue = (buckets[wordNum] ushr bucketShift) and bucketMask
            append(bucketValue)
        }
    }

    private fun getHashBoolean(hash: Int): AtomicBoolean {
        return hashBooleans[hash % hashBooleans.size]
    }

    private fun calcBucketInfo(hash: Int): Triple<Int, Int, Long> {
        val wordNum = hash ushr 4
        val bucketShift = (hash and 0x0F) shl 2  // hash.rem(16) shl 2
        val bucketMask = BUCKET_MAX_VALUE.toLong() shl bucketShift

        return Triple(wordNum, bucketShift, bucketMask)
    }
}
