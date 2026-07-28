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
 * [packMessage]와 동일한 바이트입니다. 쓰기는 target의 현재 위치에서 시작하므로 호출자는
 * nonzero position and a bounded limit. On success, the target's byte order, limit, and capacity are preserved, and
 * its position advances by the returned byte count. The exact encoded size is preflighted against
 * [ByteBuffer.remaining]; a read-only target is rejected before packing or checking the encoded size. These preflight
 * failures leave the target state and content unchanged.
 *
 * 나중의 쓰기 실패는 target 위치만 복구합니다. 이미 기록된 내용은 의도적으로 지정하지 않습니다. 이후
 * such a failure, callers must clear, reinitialize, or discard the target before reusing it. The target remains
 * caller-owned and must be thread-confined or otherwise synchronized by its caller.
 *
 * @throws ReadOnlyBufferException when [target] is read-only.
 * @throws BufferOverflowException when [target] has fewer remaining bytes than the packed message requires.
 */
fun <T: Message> packMessageTo(message: T, target: ByteBuffer): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    return writePackedAnyTo(ProtoAny.pack(message), target) { packed, buffer ->
        CodedOutputStream.newInstance(buffer).also {
            packed.writeTo(it)
            it.flush()
        }
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
 * 일치하지 않는 `Any` 타입은 `null`을 반환합니다. 잘못된 wire byte는 parse 실패를 그대로 전파합니다.
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
