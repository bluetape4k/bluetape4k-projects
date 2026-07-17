package io.bluetape4k.avro

import io.bluetape4k.io.ByteBufferOutputStream
import java.io.FilterOutputStream
import java.io.IOException
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
        FixedTargetOutputStream(ByteBufferOutputStream.fixed(view)).use(write)
        val written = view.position() - start
        target.position(start + written)
        written
    } catch (failure: Throwable) {
        target.position(start)
        throw failure
    }
}

internal fun Throwable.escapingBufferFailure(): Throwable? {
    var current: Throwable? = this
    var targetOverflow = false
    var depth = 0
    while (current != null && depth++ < 64) {
        if (current is Error) return current
        if (current is FixedTargetOverflowSignal) targetOverflow = true
        current = current.cause?.takeUnless { it === current }
    }
    return if (targetOverflow) {
        BufferOverflowException().apply { initCause(this@escapingBufferFailure) }
    } else {
        null
    }
}

internal class FixedTargetOutputStream(
    output: OutputStream,
): FilterOutputStream(output) {

    override fun write(b: Int) = translateOverflow { super.write(b) }

    override fun write(b: ByteArray, off: Int, len: Int) =
        translateOverflow { super.write(b, off, len) }

    private inline fun translateOverflow(write: () -> Unit) {
        try {
            write()
        } catch (failure: BufferOverflowException) {
            throw FixedTargetOverflowSignal(failure)
        }
    }
}

private class FixedTargetOverflowSignal(cause: BufferOverflowException): IOException(cause)
