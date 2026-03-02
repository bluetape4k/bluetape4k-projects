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
 * ## 동작/계약
 * - [contains]가 `false`면 미포함이 보장되며, `true`는 오탐 가능성이 있습니다.
 * - 삭제는 지원하지 않으며 삭제 가능한 구현은 [MutableBloomFilter]를 사용합니다.
 * - [m], [k]는 비트 배열 크기/해시 함수 수를 나타내며 구현체 생성 시 결정됩니다.
 *
 * ```kotlin
 * val filter = InMemoryBloomFilter<String>()
 * filter.add("alpha")
 * // filter.contains("alpha") == true
 * ```
 *
 * @param T 요소의 수형
 *
 * @see [MutableBloomFilter]
 * @see [SuspendBloomFilter]
 */
interface BloomFilter<T: Any> {

    /** Bloom filter 비트 배열 크기입니다. */
    val m: Int

    /** 해시 함수 개수입니다. */
    val k: Int

    val isEmpty: Boolean

    /**
     * 원소를 추가합니다.
     *
     * ## 동작/계약
     * - 입력 원소로 계산한 해시 오프셋 비트를 설정합니다.
     * - 수신 필터 상태를 변경합니다.
     */
    fun add(value: T)

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - 해시 오프셋의 모든 비트가 설정되어 있으면 `true`를 반환합니다.
     * - `true`는 오탐일 수 있고 `false`는 미포함이 확정입니다.
     *
     * ```kotlin
     * filter.add("alpha")
     * // filter.contains("alpha") == true
     * ```
     */
    fun contains(value: T): Boolean

    /**
     * 구현체 기준 원소 수 또는 내부 비트 길이를 반환합니다.
     */
    fun count(): Long

    /**
     * 필터 상태를 초기화합니다.
     */
    fun clear()

    /**
     * 원소를 검사할 때 `k` 개의 해시값이 모두 1이 될 확률
     *
     * ## 동작/계약
     * - [n]을 기준으로 이론적 오탐 확률을 계산합니다.
     * - [n]은 0 이상을 가정하며 검증은 수행하지 않습니다.
     *
     * ```kotlin
     * val p = filter.getFalsePositiveProbability(10_000)
     * // p in 0.0..1.0
     * ```
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
