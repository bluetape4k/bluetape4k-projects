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
 *
 * @see [BloomFilter]
 */
interface MutableBloomFilter<T: Any>: BloomFilter<T> {

    /** 원소를 제거합니다. */
    fun remove(value: T)

    /** 원소의 근사 빈도를 반환합니다. */
    fun approximateCount(value: T): Int
}
