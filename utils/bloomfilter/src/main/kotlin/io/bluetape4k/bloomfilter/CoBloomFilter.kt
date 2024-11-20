package io.bluetape4k.bloomfilter

import kotlin.math.pow

/**
 * Coroutines 방식으로 동작하는 Bloom Filter
 *
 * @see [BloomFilter]
 */
interface CoBloomFilter<T: Any> {

    /**
     * Maximum bit size
     */
    val m: Int

    /**
     * Hash function count
     */
    val k: Int

    val isEmpty: Boolean

    suspend fun add(value: T)

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
    suspend fun contains(value: T): Boolean

    fun count(): Long

    suspend fun clear()

    /**
     * 원소를 검사할 때 `k` 개의 해시값이 모두 1이 될 확률
     *
     * @param n 원소의 갯수
     */
    fun getFalsePositiveProbability(n: Int): Double = 1.0 - getBitZeroProbability(n).pow(k)

    /**
     * bloom filter에 원소 n개 추가했을 경우, 특정 bit가 0일 확률
     *
     * @param n 원소의 갯수
     * @return 특정 bit가 0일 확률
     */
    fun getBitZeroProbability(n: Int): Double = (1.0 - 1.0 / m.toDouble()).pow(k * n)

    /**
     * bloom filter에 원소 n개가 추가 되었을 경우, 원소당 bit 수
     *
     * @param n 원소의 갯수
     * @return 원소 당 비트 수
     */
    fun getBitsPerElement(n: Int): Double = m.toDouble() / n.toDouble()

}
