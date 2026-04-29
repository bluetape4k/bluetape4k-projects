package io.bluetape4k.probabilistic.bloomfilter

/**
 * 병합 가능한 인메모리 Bloom Filter입니다.
 *
 * 삭제 가능한 Counting Bloom Filter가 아니라 [InMemoryBloomFilter]와 같은 bitset OR 병합 구현입니다.
 */
class InMemoryMutableBloomFilter<T: Any>(
    config: BloomFilterConfig = BloomFilterConfig(),
    hasher: BloomHasher<T> = DefaultBloomHasher,
): InMemoryBloomFilter<T>(config, hasher)
