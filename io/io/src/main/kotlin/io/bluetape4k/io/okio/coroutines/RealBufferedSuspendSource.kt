package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.ByteString
import okio.EOFException
import okio.Options
import okio.Timeout
import java.util.concurrent.atomic.AtomicBoolean

internal class RealBufferedSuspendSource(
    private val source: SuspendSource,
): BufferedSuspendSource {

    companion object: KLoggingChannel()

    private val closed = AtomicBoolean(false)
    override val buffer = Buffer()

    override suspend fun exhausted(): Boolean {
        checkNotClosed()
        return buffer.exhausted() && source.read(buffer, SEGMENT_SIZE) == -1L
    }

    override suspend fun require(byteCount: Long) {
        if (!request(byteCount)) {
            throw EOFException("source exhausted. Required $byteCount bytes")
        }
    }

    override suspend fun request(byteCount: Long): Boolean {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        checkNotClosed()
        while (buffer.size < byteCount) {
            if (source.read(buffer, SEGMENT_SIZE) == -1L) {
                return false
            }
        }
        return true
    }

    override suspend fun readByte(): Byte {
        require(1L)
        return buffer.readByte()
    }

    override suspend fun readShort(): Short {
        require(2L)
        return buffer.readShort()
    }

    override suspend fun readShortLe(): Short {
        require(2L)
        return buffer.readShortLe()
    }

    override suspend fun readInt(): Int {
        require(4L)
        return buffer.readInt()
    }

    override suspend fun readIntLe(): Int {
        require(4L)
        return buffer.readIntLe()
    }

    override suspend fun readLong(): Long {
        require(8L)
        return buffer.readLong()
    }

    override suspend fun readLongLe(): Long {
        require(8L)
        return buffer.readLongLe()
    }

    override suspend fun readDecimalLong(): Long {
        require(1L)

        var pos = 0L
        while (request(pos + 1L)) {
            val b = buffer[pos].toInt()
            if (
                (b < '0'.code || b > '9'.code) &&
                (pos != 0L || b != '-'.code)
            ) {
                if (pos == 0L) {
                    throw NumberFormatException("Expected leading [0-9] or '-' character but was 0x${b.toString(16)}")
                }
                break
            }
            pos++
        }
        return buffer.readDecimalLong()
    }

    override suspend fun readHexadecimalUnsignedLong(): Long {
        require(1L)

        var pos = 0L
        while (request(pos + 1L)) {
            val b = buffer[pos].toInt()
            if (
                (b < '0'.code || b > '9'.code) &&
                (b < 'a'.code || b > 'f'.code) &&
                (b < 'A'.code || b > 'F'.code)
            ) {
                if (pos == 0L) {
                    throw NumberFormatException("Expected leading [0-9a-fA-F] character but was 0x${b.toString(16)}")
                }
                break
            }
            pos++
        }
        return buffer.readHexadecimalUnsignedLong()
    }

    override suspend fun skip(byteCount: Long) {
        checkNotClosed()
        var remaining = byteCount
        while (remaining > 0L) {
            if (buffer.size == 0L && source.read(buffer, SEGMENT_SIZE) == -1L) {
                throw EOFException("source exhausted. buffer size is 0 and cannot read more")
            }
            val toSkip = minOf(remaining, buffer.size)
            buffer.skip(toSkip)
            remaining -= toSkip
        }
    }

    override suspend fun readByteString(): ByteString {
        checkNotClosed()
        buffer.writeAll(source)
        return buffer.readByteString()
    }

    override suspend fun readByteString(byteCount: Long): ByteString {
        require(byteCount)
        return buffer.readByteString(byteCount)
    }

    override suspend fun select(options: Options): Int {
        TODO("Not yet implemented")
//    checkNotClosed()
//
//    while (true) {
//      val index = buffer.selectPrefix(options, selectTruncated = true)
//      when (index) {
//        -1 -> {
//          return -1
//        }
//        -2 -> {
//          // We need to grow the buffer. Do that, then try it all again.
//          if (source.read(buffer, SEGMENT_SIZE) == -1L) return -1
//        }
//        else -> {
//          // We matched a full byte string: consume it and return it.
//          val selectedSize = options.byteStrings[index].size
//          buffer.skip(selectedSize.toLong())
//          return index
//        }
//      }
//    }
    }

    override suspend fun readByteArray(): ByteArray {
        checkNotClosed()
        buffer.writeAll(source)
        return buffer.readByteArray()
    }

    override suspend fun readByteArray(byteCount: Long): ByteArray {
        require(byteCount)
        return buffer.readByteArray(byteCount)
    }

    override suspend fun read(sink: ByteArray): Int {
        return read(sink, 0, sink.size)
    }

    override suspend fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        // TODO checkOffsetAndCount(sink.size.toLong(), offset.toLong(), byteCount.toLong())
        checkNotClosed()
        if (buffer.size == 0L) {
            val read = source.read(buffer, SEGMENT_SIZE)
            if (read == -1L) {
                return -1
            }
        }
        val toRead = minOf(byteCount.toLong(), buffer.size).toInt()
        return buffer.read(sink, offset, toRead)
    }

    override suspend fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        checkNotClosed()

        if (buffer.size == 0L) {
            val read = source.read(buffer, SEGMENT_SIZE)
            if (read == -1L) {
                return -1L
            }
        }
        val toRead = minOf(byteCount, buffer.size)
        return buffer.read(sink, toRead)
    }

    override suspend fun readFully(sink: ByteArray) {
        try {
            require(sink.size.toLong())
        } catch (e: EOFException) {
            // The underlying source is exhausted. Copy the bytes we got before rethrowing.
            var offset = 0
            while (buffer.size > 0L) {
                val read = buffer.read(sink, offset, buffer.size.toInt())
                if (read == -1) throw AssertionError()
                offset += read
            }
            throw e
        }
        buffer.readFully(sink)
    }

    override suspend fun readFully(sink: Buffer, byteCount: Long) {
        try {
            require(byteCount)
        } catch (e: EOFException) {
            // The underlying source is exhausted. Copy the bytes we got before rethrowing.
            sink.writeAll(buffer)
            throw e
        }
        buffer.readFully(sink, byteCount)
    }

    override suspend fun readAll(sink: SuspendSink): Long {
        checkNotClosed()
        var totalBytesWritten = 0L
        while (source.read(buffer, SEGMENT_SIZE) != -1L) {
            val emitByteCount = buffer.completeSegmentByteCount()
            if (emitByteCount > 0L) {
                totalBytesWritten += emitByteCount
                sink.write(buffer, emitByteCount)
            }
        }
        if (buffer.size > 0L) {
            totalBytesWritten += buffer.size
            sink.write(buffer, buffer.size)
        }
        return totalBytesWritten
    }

    override suspend fun readUtf8(): String {
        checkNotClosed()
        buffer.writeAll(source)
        return buffer.readUtf8()
    }

    override suspend fun readUtf8(byteCount: Long): String {
        require(byteCount)
        return buffer.readUtf8(byteCount)
    }

    override suspend fun readUtf8Line(): String? {
        val newline = indexOf('\n'.code.toByte())

        return if (newline != -1L) {
            if (buffer.size != 0L) {
                readUtf8(buffer.size)
            } else {
                null
            }
        } else {
            buffer.readUtf8Line(newline)
        }
    }

    override suspend fun readUtf8LineStrict(): String {
        return readUtf8LineStrict(Long.MAX_VALUE)
    }

    override suspend fun readUtf8LineStrict(limit: Long): String {
        limit.requireZeroOrPositiveNumber("limit")
        val scanLength = if (limit == Long.MAX_VALUE) Long.MAX_VALUE else limit + 1L
        val newline = indexOf('\n'.code.toByte(), 0, scanLength)
        if (newline != -1L) {
            return buffer.readUtf8Line(newline)
        }
        if (scanLength < Long.MAX_VALUE &&
            request(scanLength) &&
            buffer[scanLength - 1].toInt() == '\r'.code &&
            buffer[scanLength].toInt() == '\n'.code) {
            return buffer.readUtf8(scanLength)      // The line was 'limit' UTF-8 bytes followed by \r\n.
        }
        val data = Buffer()
        buffer.copyTo(data, 0, minOf(32, buffer.size))
        throw EOFException("\\n not found: limit=${minOf(buffer.size, limit)}, count=${data.readByteString().hex()}...")
    }

    override suspend fun readUtf8CodePoint(): Int {
        require(1L)

        val b0 = buffer[0].toInt()
        when {
            b0 and 0xE0 == 0xC0 -> require(2)
            b0 and 0xF0 == 0xE0 -> require(3)
            b0 and 0xF8 == 0xF0 -> require(4)
        }
        return buffer.readUtf8CodePoint()
    }

    override suspend fun indexOf(b: Byte, fromIndex: Long, toIndex: Long): Long {
        checkNotClosed()
        fromIndex.requireInRange(0, toIndex, "fromIndex")

        var current = fromIndex
        while (current < toIndex) {
            val result = buffer.indexOf(b, current, toIndex)
            if (result != -1L) {
                return result
            }

            // The byte wasn't in the buffer. Give up if we've already reached our target size or if the
            // underlying stream is exhausted.
            val lastBufferSize = buffer.size
            if (lastBufferSize >= toIndex || source.read(buffer, SEGMENT_SIZE) == -1L) {
                return -1L
            }
            // Continue the search from where we left off.
            current = maxOf(current, lastBufferSize)
        }
        return -1L
    }

    override suspend fun indexOf(bytes: ByteString, fromIndex: Long): Long {
        checkNotClosed()
        var current = fromIndex
        while (true) {
            val result = buffer.indexOf(bytes, current)
            if (result != -1L) {
                return result
            }

            val lastBufferSize = buffer.size
            if (source.read(buffer, SEGMENT_SIZE) == -1L) {
                return -1L
            }

            // Keep searching, picking up from where we left off.
            current = maxOf(current, lastBufferSize)
        }
    }

    override suspend fun indexOfElement(targetBytes: ByteString, fromIndex: Long, toIndex: Long): Long {
        TODO("Not yet implemented")
    }

    override suspend fun rangeEquals(offset: Long, bytes: ByteString, bytesOffset: Int, byteCount: Int): Boolean {
        checkNotClosed()
        if (offset < 0L || bytesOffset < 0L || byteCount < 0L || bytes.size - bytesOffset < byteCount) {
            return false
        }
        for (i in 0 until byteCount) {
            val bufferOffset = offset + i
            if (!request(bufferOffset + 1L)) return false
            if (buffer[bufferOffset] != bytes[bytesOffset + i]) return false
        }
        return true
    }

    override fun peek(): BufferedSuspendSource {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun timeout(): Timeout {
        return source.timeout()
    }

    private fun checkNotClosed() {
        check(!closed.get()) { "RealBufferedSuspendSource is closed" }
    }

    internal fun Buffer.readUtf8Line(newline: Long): String = when {
        newline > 0L && this[newline - 1].toInt() == '\r'.code -> {
            // Read everything until '\r\n', then skip the '\r\n'.
            val result = readUtf8(newline - 1)
            skip(2L)
            result
        }

        else -> {
            // Read everything until '\n', then skip the '\n'.
            val result = readUtf8(newline)
            skip(1)
            result
        }
    }
}
