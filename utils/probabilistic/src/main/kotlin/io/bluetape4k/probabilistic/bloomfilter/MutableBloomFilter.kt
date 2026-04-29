package io.bluetape4k.probabilistic.bloomfilter

/**
 * 동일한 설정의 Bloom Filter를 병합할 수 있는 mutable Bloom Filter 계약입니다.
 *
 * 삭제 가능한 Counting Bloom Filter가 아니며, [putAll]은 같은 hash/config를 가진 필터의 bitset을 OR 병합합니다.
 */
interface MutableBloomFilter<T: Any>: BloomFilter<T> {

    /**
     * 다른 Bloom Filter의 bitset을 현재 필터로 병합합니다.
     *
     * 구현체가 서로 호환되지 않으면 [IllegalArgumentException]을 던집니다.
     */
    fun putAll(other: MutableBloomFilter<T>)
}
