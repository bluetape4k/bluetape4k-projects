package io.bluetape4k.io.serializer

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * 객체를 바이너리 형식으로 직렬화/역직렬화하는 최상위 인터페이스입니다.
 *
 * ## Null 처리 정책
 * - `serialize(null)`: 빈 [ByteArray]를 반환합니다.
 * - `deserialize(null)` 또는 `deserialize(emptyByteArray)`: `null`을 반환합니다.
 * - 그 외 직렬화/역직렬화 실패: 구현체에 따라 예외를 던질 수 있습니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val serializer = BinarySerializers.Kryo
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // "Hello, World!"
 * ```
 *
 * interface default ByteBuffer 메서드는 할당 기반 호환성 fallback입니다. 자세한 내용은
 * [issue #1039 allocation evidence](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)
 * before making backend-specific allocation claims.
 *
 * @see BinarySerializers
 * @see AbstractBinarySerializer
 */
interface BinarySerializer {

    /**
     * 객체를 Binary 방식으로 직렬화합니다.
     *
     * @param graph 직렬화할 객체
     * @return 직렬화된 데이터
     */
    fun serialize(graph: Any?): ByteArray

    /**
     * [graph]를 직렬화하고 결과 바이트를 호출자 소유 [target]에 씁니다.
     *
     * This interface default is an allocating fallback: it first obtains a [ByteArray] from [serialize], then writes
     * that array to [target]. Null input and zero-byte results retain [serialize]'s existing policy. On success, the
     * returned count is exactly the number of bytes written and is bounded by [Int.MAX_VALUE].
     *
     * target은 동기적으로 빌려 쓰며 호출 뒤 보관하지 않습니다. 이 메서드는 flush 또는 close를 수행하지 않습니다
     * it. Serializer and destination failures propagate unchanged; a failed destination may already contain partial
     * output.
     *
     * @return the exact number of bytes written
     */
    @Throws(IOException::class)
    fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
        val bytes = serialize(graph)
        target.write(bytes)
        return bytes.size
    }

    /**
     * [graph]를 호출자 소유 [target]의 현재 위치부터 직렬화합니다.
     *
     * This default is an allocating compatibility fallback: it first calls [serialize] to create a [ByteArray], then
     * copies that array into [target]. A read-only target fails with `ReadOnlyBufferException` before [serialize] is
     * invoked. A null graph follows [serialize]'s existing policy; a zero-byte result returns `0` without moving the
     * position. Insufficient remaining space fails with the raw `BufferOverflowException`.
     *
     * Success advances the position by the returned count without changing limit, capacity, or byte order. Failure
     * restores the original position and rethrows the same failure, including any `Error`; bytes overwritten before a
     * failure are unspecified and are not rolled back. Normal JDK mark invalidation rules apply to successful position
     * movement. The buffer remains caller-owned and must be thread-confined for the duration of the call.
     *
     * @return the number of bytes written
     */
    fun serializeTo(graph: Any?, target: ByteBuffer): Int =
        serializeTo(target) { serialize(graph) }

    /**
     * 직렬화된 데이터를 읽어 대상 객체로 역직렬화합니다.
     *
     * @param T     역직렬화할 객체 수형
     * @param bytes 직렬화된 데이터
     * @return 역직렬화한 객체
     */
    fun <T: Any> deserialize(bytes: ByteArray?): T?

    /**
     * `[source.position(), source.limit())` 범위의 신뢰된 호출자 한정 바이트를 역직렬화합니다.
     *
     * This default is an allocating compatibility fallback: it copies the remaining bytes to a new [ByteArray] and
     * delegates to [deserialize]. The source position, limit, mark, and byte order are preserved on success and failure;
     * heap, direct, sliced, and read-only sources are supported. The source remains caller-owned and must not be mutated
     * or shared concurrently during the call. Callers are responsible for bounding untrusted input before invocation.
     */
    fun <T: Any> deserializeFrom(source: ByteBuffer): T? =
        deserialize(copyRemaining(source))

}
