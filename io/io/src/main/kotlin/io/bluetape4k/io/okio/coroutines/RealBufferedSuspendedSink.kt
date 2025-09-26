package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.ByteString
import okio.Timeout
import java.util.concurrent.atomic.AtomicBoolean

internal class RealBufferedSuspendedSink(
    private val sink: SuspendedSink,
): BufferedSuspendedSink {

    companion object: KLoggingChannel()

    private var closed = AtomicBoolean(false)
    override val buffer = Buffer()

    override suspend fun write(byteString: ByteString): BufferedSuspendedSink {
        return emitCompleteSegments { buffer.write(byteString) }
    }

    override suspend fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedSuspendedSink {
        return emitCompleteSegments { buffer.write(source, offset, byteCount) }
    }

    override suspend fun write(source: SuspendedSource, byteCount: Long) = apply {
        checkNotClosed()
        var remaining = byteCount
        while (remaining > 0L) {
            val read = source.read(buffer, remaining)
            if (read == -1L) throw okio.EOFException()
            remaining -= read
            emitCompleteSegments()
        }
    }

    override suspend fun write(source: Buffer, byteCount: Long) {
        emitCompleteSegments { buffer.write(source, byteCount) }
    }

    override suspend fun writeAll(source: SuspendedSource): Long {
        checkNotClosed()
        var totalBytesRead = 0L
        while (true) {
            val readCount = source.read(buffer, SEGMENT_SIZE)
            if (readCount == -1L) break
            totalBytesRead += readCount
            emitCompleteSegments()
        }
        return totalBytesRead
    }

    override suspend fun writeUtf8(
        string: String,
        beginIndex: Int,
        endIndex: Int,
    ) = emitCompleteSegments {
        buffer.writeUtf8(string, beginIndex, endIndex)
    }

    override suspend fun writeUtf8CodePoint(codePoint: Int) = emitCompleteSegments {
        buffer.writeUtf8CodePoint(codePoint)
    }

    override suspend fun writeByte(b: Int) = emitCompleteSegments {
        buffer.writeByte(b)
    }

    override suspend fun writeShort(s: Int) = emitCompleteSegments {
        buffer.writeShort(s)
    }

    override suspend fun writeShortLe(s: Int) = emitCompleteSegments {
        buffer.writeShortLe(s)
    }

    override suspend fun writeInt(i: Int) = emitCompleteSegments {
        buffer.writeInt(i)
    }

    override suspend fun writeIntLe(i: Int) = emitCompleteSegments {
        buffer.writeIntLe(i)
    }

    override suspend fun writeLong(v: Long) = emitCompleteSegments {
        buffer.writeLong(v)
    }

    override suspend fun writeLongLe(v: Long) = emitCompleteSegments {
        buffer.writeLongLe(v)
    }

    override suspend fun writeDecimalLong(v: Long) = emitCompleteSegments {
        buffer.writeDecimalLong(v)
    }

    override suspend fun writeHexadecimalUnsignedLong(v: Long) = emitCompleteSegments {
        buffer.writeHexadecimalUnsignedLong(v)
    }

    override suspend fun flush() {
        checkNotClosed()
        if (buffer.size > 0L) {
            sink.write(buffer, buffer.size)
        }
        sink.flush()
    }

    override suspend fun emit() = apply {
        checkNotClosed()
        val byteCount = buffer.size
        if (byteCount > 0L) sink.write(buffer, byteCount)
    }

    override suspend fun emitCompleteSegments() = emitCompleteSegments {
        // Nothing to do 
    }

    override suspend fun close() {
        if (closed.get()) return

        // Emit buffered data to the underlying sink. If this fails, we still need
        // to close the sink; otherwise we risk leaking resources.
        var thrown: Throwable? = null
        try {
            if (buffer.size > 0L) {
                sink.write(buffer, buffer.size)
            }
        } catch (e: Throwable) {
            thrown = e
        }
        try {
            sink.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }
        closed.set(true)

        if (thrown != null) throw thrown
    }

    override suspend fun timeout(): Timeout {
        return sink.timeout()
    }

    override fun toString(): String = "buffer($sink)"

    private suspend inline fun emitCompleteSegments(block: () -> Unit): BufferedSuspendedSink = apply {
        checkNotClosed()
        block()
        val byteCount = buffer.completeSegmentByteCount()
        if (byteCount > 0L) sink.write(buffer, byteCount)
    }

    private fun checkNotClosed() {
        check(!closed.get()) { "RealBufferedSuspendSink is closed" }
    }
}
