package io.bluetape4k.protobuf

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.Message
import com.google.protobuf.kotlin.isA
import com.google.protobuf.kotlin.unpack
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

/**
 * Protobuf [Message]를 `Any`로 감싸 바이트 배열로 패킹합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `ProtoAny.pack(message)` 결과를 직렬화합니다.
 * - 반환값은 새 바이트 배열입니다.
 *
 * ```kotlin
 * val bytes = packMessage(message)
 * // bytes.isNotEmpty() == true
 * ```
 */
fun <T: Message> packMessage(message: T): ByteArray {
    val any = ProtoAny.pack(message)
    return any.toByteArray()
}

/**
 * Packs [message] as protobuf `Any` wire bytes into [target].
 *
 * The bytes are identical to [packMessage]. Writing begins at the target's current position, so callers may use a
 * nonzero position and a bounded limit. On success, the target's byte order, limit, and capacity are preserved, and
 * its position advances by the returned byte count. The exact encoded size is preflighted against
 * [ByteBuffer.remaining]; a read-only target is
 * rejected before the size check. These preflight failures leave the target state and content unchanged.
 *
 * A later write failure restores only the target position; already-written content is intentionally unspecified. After
 * such a failure, callers must clear, reinitialize, or discard the target before reusing it. The target remains
 * caller-owned and must be thread-confined or otherwise synchronized by its caller.
 *
 * @throws ReadOnlyBufferException when [target] is read-only.
 * @throws BufferOverflowException when [target] has fewer remaining bytes than the packed message requires.
 */
fun <T: Message> packMessageTo(message: T, target: ByteBuffer): Int =
    writePackedAnyTo(ProtoAny.pack(message), target) { packed, buffer ->
        CodedOutputStream.newInstance(buffer).also {
            packed.writeTo(it)
            it.flush()
        }
    }

/**
 * `Any` 바이트 배열에서 지정 타입 [T] 메시지를 언패킹합니다.
 *
 * ## 동작/계약
 * - 바이트를 `ProtoAny.parseFrom(bytes)`로 파싱합니다.
 * - 실제 타입이 [T]와 다르면 `null`을 반환합니다.
 * - 파싱 실패 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val msg: MyProto? = unpackMessage(bytes)
 * // msg == null || msg is MyProto
 * ```
 */
inline fun <reified T: Message> unpackMessage(bytes: ByteArray): T? {
    val any = ProtoAny.parseFrom(bytes)
    return if (any.isA<T>()) any.unpack() else null
}

/**
 * Unpacks a protobuf `Any` from the remaining bytes of [source].
 *
 * Parsing uses a short-lived duplicate, so the caller's source position, limit, mark, and byte order are preserved;
 * this also supports nonzero positions and bounded sources. No buffer view is retained after this function returns.
 * A mismatched `Any` type returns `null`; malformed wire bytes propagate their parse failure.
 */
inline fun <reified T: Message> unpackMessage(source: ByteBuffer): T? {
    val any = ProtoAny.parseFrom(source.duplicate())
    return if (any.isA<T>()) any.unpack() else null
}

/**
 * Writes an already-packed `Any` to [target] through [write].
 *
 * This internal seam keeps the preflight and position-only rollback contract testable. It does not provide a content
 * rollback after [write] has started; callers must clear, reinitialize, or discard the target after a non-preflight
 * failure.
 */
internal fun writePackedAnyTo(
    packed: ProtoAny,
    target: ByteBuffer,
    write: (ProtoAny, ByteBuffer) -> Unit,
): Int {
    if (target.isReadOnly) {
        throw ReadOnlyBufferException()
    }
    val size = packed.serializedSize
    if (target.remaining() < size) {
        throw BufferOverflowException()
    }

    val initialPosition = target.position()
    try {
        write(packed, target)
        check(target.position() - initialPosition == size) {
            "Packed Any write advanced ${target.position() - initialPosition} bytes, expected $size"
        }
        return size
    } catch (e: Throwable) {
        target.position(initialPosition)
        throw e
    }
}
