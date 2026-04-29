package io.bluetape4k.probabilistic.bloomfilter

import java.util.Arrays

/**
 * JDK/Kotlin만 사용해 구현한 인메모리 Bloom Filter입니다.
 *
 * ## 동작/계약
 * - 내부 저장소는 [LongArray] bitset입니다.
 * - 해시 offset은 SHA-256 기반 double hashing으로 계산합니다.
 * - thread-safe를 보장하지 않으므로 동시 쓰기가 필요하면 외부에서 동기화해야 합니다.
 */
open class InMemoryBloomFilter<T: Any>(
    private val config: BloomFilterConfig = BloomFilterConfig(),
    private val hasher: BloomHasher<T> = DefaultBloomHasher,
): MutableBloomFilter<T> {

    override val expectedInsertions: Long = config.expectedInsertions
    override val falsePositiveProbability: Double = config.falsePositiveProbability
    override val bitSize: Long = config.bitSize
    override val hashFunctionCount: Int = config.hashFunctionCount

    private val words = LongArray(wordSize(bitSize))
    private var activeBitCount: Long = 0L

    override val bitCount: Long
        get() = activeBitCount

    override fun mightContain(element: T): Boolean {
        val indexes = indexes(element)
        return indexes.all { getBit(it) }
    }

    override fun put(element: T): Boolean {
        val indexes = indexes(element)
        var changed = false

        indexes.forEach { index ->
            if (setBit(index)) {
                changed = true
                activeBitCount++
            }
        }
        return changed
    }

    override fun putAll(other: MutableBloomFilter<T>) {
        require(other is InMemoryBloomFilter<T>) {
            "other must be InMemoryBloomFilter"
        }
        require(isCompatible(other)) {
            "BloomFilter config or hasher is not compatible"
        }

        words.indices.forEach { i ->
            words[i] = words[i] or other.words[i]
        }
        activeBitCount = words.sumOf { it.countOneBits().toLong() }
    }

    override fun approximateElementCount(): Long =
        approximateElementCount(bitCount, bitSize, hashFunctionCount)

    override fun expectedFpp(): Double =
        expectedFpp(bitCount, bitSize, hashFunctionCount)

    override fun clear() {
        Arrays.fill(words, 0L)
        activeBitCount = 0L
    }

    internal fun isCompatible(other: InMemoryBloomFilter<*>): Boolean =
        expectedInsertions == other.expectedInsertions &&
            falsePositiveProbability == other.falsePositiveProbability &&
            bitSize == other.bitSize &&
            hashFunctionCount == other.hashFunctionCount &&
            hasher == other.hasher &&
            words.size == other.words.size

    private fun indexes(element: T): LongArray =
        BloomHashSupport.indexes(hasher.bytes(element), hashFunctionCount, bitSize)

    private fun getBit(index: Long): Boolean {
        val wordIndex = (index ushr WORD_SHIFT).toInt()
        val mask = 1L shl (index and WORD_MASK).toInt()
        return words[wordIndex] and mask != 0L
    }

    private fun setBit(index: Long): Boolean {
        val wordIndex = (index ushr WORD_SHIFT).toInt()
        val mask = 1L shl (index and WORD_MASK).toInt()
        val before = words[wordIndex]
        words[wordIndex] = before or mask
        return before and mask == 0L
    }

    private companion object {
        private const val WORD_SHIFT = 6
        private const val WORD_MASK = 63L

        private fun wordSize(bitSize: Long): Int =
            ((bitSize + Long.SIZE_BITS - 1) / Long.SIZE_BITS).toInt()
    }
}
