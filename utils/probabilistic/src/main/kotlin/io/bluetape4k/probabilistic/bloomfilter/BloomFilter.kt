package io.bluetape4k.probabilistic.bloomfilter

/**
 * Bloom Filter는 원소가 집합에 포함될 가능성을 빠르게 검사하는 확률적 자료구조입니다.
 *
 * ## 동작/계약
 * - [mightContain]이 `false`이면 미포함이 보장됩니다.
 * - [mightContain]이 `true`이면 포함 가능성이 있으며 오탐(false positive)이 발생할 수 있습니다.
 * - 삭제는 지원하지 않습니다.
 * - 구현체는 기본적으로 thread-safe를 보장하지 않습니다.
 *
 * @param T 필터에 넣을 원소 타입
 */
interface BloomFilter<T: Any> {

    /** 필터 생성 시 가정한 예상 삽입 원소 수입니다. */
    val expectedInsertions: Long

    /** 필터 생성 시 목표로 한 오탐률입니다. */
    val falsePositiveProbability: Double

    /** 내부 bitset 크기입니다. */
    val bitSize: Long

    /** 원소 하나당 계산하는 해시 offset 개수입니다. */
    val hashFunctionCount: Int

    /** 현재 켜진 bit 개수입니다. */
    val bitCount: Long

    /** 필터가 비어 있는지 여부입니다. */
    val isEmpty: Boolean get() = bitCount == 0L

    /**
     * 원소 포함 가능성을 검사합니다.
     *
     * @return 미포함이 확정이면 `false`, 포함 가능성이 있으면 `true`
     */
    fun mightContain(element: T): Boolean

    /**
     * 원소를 필터에 추가합니다.
     *
     * @return 하나 이상의 bit가 새로 켜졌으면 `true`, 모든 bit가 이미 켜져 있으면 `false`.
     * `false`는 이미 존재 확정이 아니라 Bloom Filter 특성상 이미 존재할 가능성입니다.
     */
    fun put(element: T): Boolean

    /**
     * 현재 bit 포화도를 기준으로 삽입 원소 수를 근사 계산합니다.
     */
    fun approximateElementCount(): Long

    /**
     * 현재 bit 포화도를 기준으로 기대 오탐률을 계산합니다.
     */
    fun expectedFpp(): Double

    /**
     * 필터 상태를 초기화합니다.
     */
    fun clear()
}
