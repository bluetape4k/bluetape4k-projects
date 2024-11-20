package io.bluetape4k.bloomfilter

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import net.openhft.hashing.LongHashFunction
import java.io.Serializable
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * Murmur3 hashing 을 사용하는 Hasher 입니다.
 */
internal object Hasher: KLogging() {

    private val murmur3 = LongHashFunction.murmur_3()

    fun <T> murmurHashOffset(value: T, k: Int, m: Int): IntArray {
        return when (value) {
            is Int          -> murmurHashOffsetInternal(value, k, m)
            is Long         -> murmurHashOffsetInternal(value, k, m)
            is String       -> murmurHashOffsetInternal(value, k, m)
            is ByteArray    -> murmurHashOffsetInternal(value, k, m)
            is Serializable -> murmurHashOffsetInternal(value, k, m)
            else            -> murmurHashOffsetInternal(value.toString(), k, m)
        }
    }

    /**
     * Murmur3 hashing 을 사용하여 offset을 얻습니다.
     *
     * @param value
     * @param k  offset array size
     * @param m  maximum offset size
     * @return
     */
    internal fun murmurHashOffsetInternal(value: Int, k: Int, m: Int): IntArray {
        return calcHashOffset(value, k, m) { murmur3.hashInt(it) }
    }

    /**
     * Murmur3 hashing 을 사용하여 offset을 얻습니다.
     *
     * @param value
     * @param k  offset array size
     * @param m  maximum offset size
     * @return
     */
    internal fun murmurHashOffsetInternal(value: Long, k: Int, m: Int): IntArray {
        return calcHashOffset(value, k, m) { murmur3.hashLong(it) }
    }

    /**
     * Murmur3 hashing 을 사용하여 offset을 얻습니다.
     *
     * @param value
     * @param k  offset array size
     * @param m  maximum offset size
     * @return
     */
    internal fun murmurHashOffsetInternal(value: String, k: Int, m: Int): IntArray {
        return calcHashOffset(value, k, m) { murmur3.hashChars(it) }
    }

    internal fun murmurHashOffsetInternal(value: ByteArray, k: Int, m: Int): IntArray {
        return calcHashOffset(value, k, m) { murmur3.hashBytes(it) }
    }

    internal fun murmurHashOffsetInternal(value: Serializable, k: Int, m: Int): IntArray {
        return calcHashOffset(value, k, m) {
            val bytes = BinarySerializers.Jdk.serialize(it)
            murmur3.hashBytes(bytes)
        }
    }

    private inline fun <T> calcHashOffset(value: T, k: Int, m: Int, hashSupplier: (T) -> Long): IntArray {
        val hash = hashSupplier(value)
        return IntArray(k) {
            ((hash + (31.0.pow(it) - 1) * hash) % m).absoluteValue.toInt()
        }
    }
}
