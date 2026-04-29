package io.bluetape4k.probabilistic.bloomfilter

/**
 * 인메모리 Bloom Filter를 생성합니다.
 *
 * @param expectedInsertions 예상 삽입 원소 수
 * @param fpp 목표 오탐률
 * @param hasher 원소를 hash 입력 byte 배열로 변환하는 전략
 */
fun <T: Any> bloomFilter(
    expectedInsertions: Long = 1_000_000L,
    fpp: Double = 0.03,
    hasher: BloomHasher<T> = DefaultBloomHasher,
): InMemoryBloomFilter<T> =
    InMemoryBloomFilter(BloomFilterConfig(expectedInsertions, fpp), hasher)

/**
 * 병합 가능한 인메모리 Bloom Filter를 생성합니다.
 */
fun <T: Any> mutableBloomFilter(
    expectedInsertions: Long = 1_000_000L,
    fpp: Double = 0.03,
    hasher: BloomHasher<T> = DefaultBloomHasher,
): InMemoryMutableBloomFilter<T> =
    InMemoryMutableBloomFilter(BloomFilterConfig(expectedInsertions, fpp), hasher)

/**
 * suspend API를 제공하는 인메모리 Bloom Filter를 생성합니다.
 */
fun <T: Any> suspendBloomFilter(
    expectedInsertions: Long = 1_000_000L,
    fpp: Double = 0.03,
    hasher: BloomHasher<T> = DefaultBloomHasher,
): InMemorySuspendBloomFilter<T> =
    InMemorySuspendBloomFilter(BloomFilterConfig(expectedInsertions, fpp), hasher)
