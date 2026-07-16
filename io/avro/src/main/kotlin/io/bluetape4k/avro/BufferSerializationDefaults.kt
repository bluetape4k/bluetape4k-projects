package io.bluetape4k.avro

import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

internal inline fun serializeTo(
    target: ByteBuffer,
    produce: () -> ByteArray,
): Int = serializeNullableTo(target, produce)

internal inline fun serializeNullableTo(
    target: ByteBuffer,
    produce: () -> ByteArray?,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    val start = target.position()
    return try {
        val bytes = produce()
        if (bytes == null) {
            0
        } else {
            if (bytes.size > target.remaining()) throw BufferOverflowException()
            target.put(bytes)
            bytes.size
        }
    } catch (failure: Throwable) {
        target.position(start)
        throw failure
    }
}

internal fun copyRemaining(source: ByteBuffer): ByteArray =
    ByteArray(source.remaining()).also { source.duplicate().get(it) }
