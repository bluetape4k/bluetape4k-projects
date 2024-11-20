package io.bluetape4k.bloomfilter

/**
 * 요소를 삭제할 수 있는 Bloom Filter
 *
 * @see [BloomFilter]
 *
 * @see [BloomFilter]
 */
interface MutableBloomFilter<T: Any>: BloomFilter<T> {

    fun remove(value: T)

    fun approximateCount(value: T): Int
}
