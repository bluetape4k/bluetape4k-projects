package io.bluetape4k.bloomfilter

import kotlin.math.pow

/**
 * 코루틴 기반 Bloom filter 계약입니다.
 *
 * ## 동작/계약
 * - [contains]의 `false`는 미포함을 보장하고, `true`는 오탐 가능성이 있습니다.
 * - suspend API이지만 구현체에 따라 실제 I/O 없이 메모리 연산일 수 있습니다.
 * - 삭제는 지원하지 않으며 삭제가 필요하면 mutable 구현을 사용해야 합니다.
 *
 * ```kotlin
 * val filter = InMemorySuspendBloomFilter<String>(m = 1024, k = 4)
 * filter.add("alpha")
 * // filter.contains("alpha") == true
 * ```
 *
 * @see [BloomFilter]
 */
interface SuspendBloomFilter<T: Any> {

    /** Bloom filter 비트 배열 크기입니다. */
    val m: Int

    /** 해시 함수 개수입니다. */
    val k: Int

    /** 필터에 어떤 원소도 추가되지 않은 초기 상태인지 여부입니다. */
    val isEmpty: Boolean

    /**
     * 원소를 추가합니다.
     *
     * ## 동작/계약
     * - 입력 원소로 계산한 해시 오프셋 비트를 설정합니다.
     * - 수신 필터 상태를 변경합니다.
     *
     * @param value 추가할 원소
     */
    suspend fun add(value: T)

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - 해시 오프셋의 모든 비트가 설정되어 있으면 `true`를 반환합니다.
     * - `true`는 오탐 가능성이 있으며 `false`는 미포함 확정입니다.
     *
     * ```kotlin
     * filter.add("alpha")
     * // filter.contains("alpha") == true
     * ```
     *
     * @param value 검사할 원소
     * @return 포함 가능성이 있으면 `true`, 미포함이 확정이면 `false`
     */
    suspend fun contains(value: T): Boolean

    /**
     * 구현체 기준 원소 수 또는 내부 비트 길이를 반환합니다.
     */
    suspend fun count(): Long

    /**
     * 필터 상태를 초기화합니다.
     */
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
