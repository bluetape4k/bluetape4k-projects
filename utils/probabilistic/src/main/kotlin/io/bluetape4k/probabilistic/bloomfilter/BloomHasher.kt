package io.bluetape4k.probabilistic.bloomfilter

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Bloom Filter 원소를 안정적인 byte 배열로 변환하는 전략입니다.
 *
 * 같은 원소는 항상 같은 byte 배열을 반환해야 하며, [InMemoryBloomFilter.putAll] 병합 대상 필터는 같은 hasher를 사용해야 합니다.
 */
fun interface BloomHasher<in T: Any> {
    /** 원소를 hash 입력 byte 배열로 변환합니다. */
    fun bytes(element: T): ByteArray
}

private const val BYTE_SHIFT = 8
private const val BYTE_MASK = 0xFFL

/**
 * 기본 Bloom Filter hash 입력 변환기입니다.
 *
 * `String`, `Int`, `Long`, `ByteArray`, [Serializable]을 직접 지원하고, 나머지는 `toString()` 결과를 UTF-8로 변환합니다.
 */
object DefaultBloomHasher: BloomHasher<Any> {

    override fun bytes(element: Any): ByteArray = when (element) {
        is String       -> element.toByteArray(StandardCharsets.UTF_8)
        is Int          -> element.toBytes()
        is Long         -> element.toBytes()
        is ByteArray    -> element
        is Serializable -> serializeOrString(element)
        else            -> element.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun serializeOrString(element: Serializable): ByteArray =
        runCatching { serialize(element) }
            .getOrElse { element.toString().toByteArray(StandardCharsets.UTF_8) }

    private fun serialize(element: Serializable): ByteArray {
        val out = ByteArrayOutputStream()
        ObjectOutputStream(out).use { it.writeObject(element) }
        return out.toByteArray()
    }

    private fun Int.toBytes(): ByteArray = ByteArray(Int.SIZE_BYTES) { index ->
        (this ushr ((Int.SIZE_BYTES - index - 1) * BYTE_SHIFT)).toByte()
    }

    private fun Long.toBytes(): ByteArray = ByteArray(Long.SIZE_BYTES) { index ->
        (this ushr ((Long.SIZE_BYTES - index - 1) * BYTE_SHIFT)).toByte()
    }
}

internal object BloomHashSupport {

    private const val HASH_ALGORITHM = "SHA-256"
    private val digestThreadLocal = ThreadLocal.withInitial { MessageDigest.getInstance(HASH_ALGORITHM) }

    fun indexes(bytes: ByteArray, hashFunctionCount: Int, bitSize: Long): LongArray {
        val digest = digestThreadLocal.get()
        digest.reset()
        val hash = digest.digest(bytes)
        val hash1 = hash.longAt(0)
        val hash2 = hash.longAt(8).let { if (it == 0L) 0x9E3779B97F4A7C15UL.toLong() else it }

        return LongArray(hashFunctionCount) { index ->
            (hash1 + index * hash2).floorMod(bitSize)
        }
    }

    private fun ByteArray.longAt(offset: Int): Long =
        (offset until offset + Long.SIZE_BYTES).fold(0L) { result, index ->
            (result shl BYTE_SHIFT) or (this[index].toLong() and BYTE_MASK)
        }

    private fun Long.floorMod(modulus: Long): Long {
        val result = this % modulus
        return if (result >= 0) result else result + modulus
    }
}
