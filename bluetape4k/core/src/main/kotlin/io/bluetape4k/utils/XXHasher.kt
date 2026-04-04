package io.bluetape4k.utils

import io.bluetape4k.support.toUtf8Bytes
import net.jpountz.xxhash.StreamingXXHash32
import net.jpountz.xxhash.XXHashFactory
import java.nio.ByteBuffer

/**
 * LZ4의 XXHash를 이용하여, 빠르고 충돌이 적은 해시 코드를 생성합니다.
 *
 * [StreamingXXHash32]가 상태를 가지므로 [ThreadLocal]을 사용하여 thread-safety를 보장합니다.
 *
 * ```kotlin
 * // 단일 값 해시
 * val hash1 = XXHasher.hash("hello")          // 결정적(deterministic) Int 값
 *
 * // 복합 값 해시 — 여러 필드를 결합한 복합 키
 * val hash2 = XXHasher.hash("user", 42L, true) // 순서에 민감
 *
 * // null 포함
 * val hash3 = XXHasher.hash(null, "world")    // null은 0으로 처리됨
 * ```
 *
 * @see [lz4-java](https://github.com/jpountz/lz4-java)
 */
object XXHasher {

    private val factory: XXHashFactory = XXHashFactory.fastestInstance()
    private const val DEFAULT_SEED = 0

    private val hash32: ThreadLocal<StreamingXXHash32> = ThreadLocal.withInitial {
        factory.newStreamingHash32(DEFAULT_SEED)
    }

    private const val ZERO_HASH = 0

    /**
     * 주어진 값들로부터 XXHash 해시 코드를 생성합니다.
     *
     * @param values 해시할 값들 (null 허용)
     * @return 해시 코드 값
     */
    @JvmStatic
    fun hash(vararg values: Any?): Int {
        if (values.isEmpty()) return 0

        val hasher = hash32.get()
        hasher.reset()

        values.forEach {
            val bytes = it.hashToBytes()
            hasher.update(bytes, 0, bytes.size)
        }
        return hasher.value
    }

    /**
     * 객체의 해시 코드를 바이트 배열로 변환합니다.
     */
    private fun Any?.hashToBytes(): ByteArray {
        val hashCode = when (this) {
            null -> ZERO_HASH
            is Enum<*> -> ordinal.hashCode()
            is String -> this.toUtf8Bytes().contentHashCode()
            else -> hashCode()
        }
        return ByteBuffer.allocate(4).putInt(hashCode).array()
    }
}
