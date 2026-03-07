package io.bluetape4k.bloomfilter

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import net.openhft.hashing.LongHashFunction
import java.io.Serializable
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * Murmur3 해싱을 사용하여 Bloom Filter 오프셋을 계산하는 유틸리티입니다.
 *
 * ## 동작/계약
 * - [murmurHashOffset]은 입력값에 대해 `k`개의 해시 오프셋을 `[0, m)` 범위로 생성합니다.
 * - 입력 타입([Int], [Long], [String], [ByteArray], [Serializable])별로 최적화된 해시 경로를 사용합니다.
 * - 그 외 타입은 `toString()` 변환 후 문자열 해시를 적용합니다.
 */
internal object Hasher: KLogging() {

    private val murmur3 = LongHashFunction.murmur_3()

    /**
     * 입력값에 대해 Murmur3 해시 기반 오프셋 배열을 생성합니다.
     *
     * @param value 해시할 원소
     * @param k 생성할 오프셋 개수 (해시 함수 수)
     * @param m 오프셋 최대 범위 (비트 배열 크기)
     * @return `[0, m)` 범위의 오프셋 배열 (크기 `k`)
     */
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
