package io.bluetape4k.bloomfilter

/**
 * 삭제 연산을 지원하는 Bloom filter 계약입니다.
 *
 * ## 동작/계약
 * - [remove]는 구현체 정책에 따라 카운팅 비트를 감소시켜 원소를 제거합니다.
 * - [approximateCount]는 원소 존재 횟수의 근사값을 반환하며 정확 카운트를 보장하지 않습니다.
 *
 * ```kotlin
 * val filter = InMemoryMutableBloomFilter()
 * filter.add("alpha")
 * filter.remove("alpha")
 * // filter.contains("alpha") == false
 * ```
 *
 * @see [BloomFilter]
 */
interface MutableBloomFilter<T: Any>: BloomFilter<T> {

    /**
     * 원소를 제거합니다.
     *
     * ## 동작/계약
     * - 카운팅 버킷의 값을 감소시켜 원소를 논리적으로 제거합니다.
     * - 존재하지 않는 원소에 대해 호출하면 아무 동작도 하지 않습니다.
     *
     * @param value 제거할 원소
     */
    fun remove(value: T)

    /**
     * 원소의 근사 빈도를 반환합니다.
     *
     * ## 동작/계약
     * - 해시 오프셋의 카운팅 버킷 중 최솟값을 반환합니다.
     * - 원소가 존재하지 않으면 `0`을 반환합니다.
     * - 오탐으로 인해 실제보다 높은 값이 반환될 수 있습니다.
     *
     * @param value 빈도를 조회할 원소
     * @return 원소의 근사 빈도 (0 이상)
     */
    fun approximateCount(value: T): Int
}
