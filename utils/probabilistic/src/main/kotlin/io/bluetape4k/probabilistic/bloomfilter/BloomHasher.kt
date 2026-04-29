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

    private fun Int.toBytes(): ByteArray =
        byteArrayOf(
            (this ushr 24).toByte(),
            (this ushr 16).toByte(),
            (this ushr 8).toByte(),
            this.toByte(),
        )

    private fun Long.toBytes(): ByteArray =
        byteArrayOf(
            (this ushr 56).toByte(),
            (this ushr 48).toByte(),
            (this ushr 40).toByte(),
            (this ushr 32).toByte(),
            (this ushr 24).toByte(),
            (this ushr 16).toByte(),
            (this ushr 8).toByte(),
            this.toByte(),
        )
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
        ((this[offset].toLong() and 0xFFL) shl 56) or
            ((this[offset + 1].toLong() and 0xFFL) shl 48) or
            ((this[offset + 2].toLong() and 0xFFL) shl 40) or
            ((this[offset + 3].toLong() and 0xFFL) shl 32) or
            ((this[offset + 4].toLong() and 0xFFL) shl 24) or
            ((this[offset + 5].toLong() and 0xFFL) shl 16) or
            ((this[offset + 6].toLong() and 0xFFL) shl 8) or
            (this[offset + 7].toLong() and 0xFFL)

    private fun Long.floorMod(modulus: Long): Long {
        val result = this % modulus
        return if (result >= 0) result else result + modulus
    }
}
