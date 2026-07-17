package io.bluetape4k.avro

import io.bluetape4k.io.ByteBufferOutputStream
import java.io.OutputStream
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

internal inline fun writeToFixedBuffer(
    target: ByteBuffer,
    write: (OutputStream) -> Unit,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()

    val start = target.position()
    val view = target.duplicate()
    return try {
        ByteBufferOutputStream.fixed(view).use(write)
        val written = view.position() - start
        target.position(start + written)
        written
    } catch (failure: Throwable) {
        target.position(start)
        throw failure.forBufferPath()
    }
}

internal fun Throwable.forBufferPath(): Throwable {
    var current: Throwable? = this
    var overflow: BufferOverflowException? = null
    var depth = 0
    while (current != null && depth++ < 64) {
        if (current is Error) return current
        if (overflow == null && current is BufferOverflowException) overflow = current
        current = current.cause?.takeUnless { it === current }
    }
    return overflow?.let {
        if (it === this) it
        else BufferOverflowException().apply { initCause(this@forBufferPath) }
    } ?: this
}
