package io.bluetape4k.bloomfilter

import kotlin.math.pow

/**
 * Bloom Filter는 특정 요소가 집합에 속하는지 여부를 검사하는데 사용하는 확률적 자료구조이다.
 *
 * 블룸 필터에 의해 어떤 원소가 집합에 속한다고 판단된 경우 실제로는 원소가 집합에 속하지 않는 긍정 오류가 발생하는 것이 가능하지만,
 * 반대로 원소가 집합에 속하지 않는 것으로 판단되었는데 실제로는 원소가 집합에 속하는 부정 오류는 절대로 발생하지 않는다는 특성이 있다.
 *
 * 집합에 원소를 추가하는 것은 가능하나, 집합에서 원소를 삭제하는 것은 불가능하다. (but, [MutableBloomFilter])
 *
 * **집합 내 원소의 숫자가 증가할수록 긍정 오류 발생 확률도 증가한다**
 *
 * 참고: [Bloom Filter](https://en.wikipedia.org/wiki/Bloom_filter)
 *
 * @param T 요소의 수형
 *
 * @see [MutableBloomFilter]
 * @see [SuspendBloomFilter]
 */
interface BloomFilter<T: Any> {

    /**
     * Maximum bit size
     */
    val m: Int

    /**
     * Hash function count
     */
    val k: Int

    val isEmpty: Boolean

    /**
     * 원소 추가
     */
    fun add(value: T)

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
    fun contains(value: T): Boolean

    /**
     * 등록된 원소의 수
     */
    fun count(): Long

    /**
     * 비트를 모두 초기화
     */
    fun clear()

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
