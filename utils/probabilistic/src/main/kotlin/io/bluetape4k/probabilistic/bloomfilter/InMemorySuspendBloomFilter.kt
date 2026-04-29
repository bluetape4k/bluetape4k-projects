package io.bluetape4k.probabilistic.bloomfilter

/**
 * [InMemoryBloomFilter]를 suspend API로 노출하는 인메모리 Bloom Filter입니다.
 *
 * 현재 구현은 dispatcher 전환이나 I/O 없이 delegate의 메모리 연산을 즉시 수행합니다.
 */
class InMemorySuspendBloomFilter<T: Any>(
    private val delegate: InMemoryBloomFilter<T> = InMemoryBloomFilter(),
): SuspendBloomFilter<T> {

    constructor(
        config: BloomFilterConfig = BloomFilterConfig(),
        hasher: BloomHasher<T> = DefaultBloomHasher,
    ): this(InMemoryBloomFilter(config, hasher))

    override val expectedInsertions: Long
        get() = delegate.expectedInsertions

    override val falsePositiveProbability: Double
        get() = delegate.falsePositiveProbability

    override val bitSize: Long
        get() = delegate.bitSize

    override val hashFunctionCount: Int
        get() = delegate.hashFunctionCount

    override val bitCount: Long
        get() = delegate.bitCount

    override suspend fun mightContain(element: T): Boolean =
        delegate.mightContain(element)

    override suspend fun put(element: T): Boolean =
        delegate.put(element)

    override suspend fun approximateElementCount(): Long =
        delegate.approximateElementCount()

    override suspend fun expectedFpp(): Double =
        delegate.expectedFpp()

    override suspend fun clear() {
        delegate.clear()
    }
}
