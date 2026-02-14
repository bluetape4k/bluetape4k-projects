package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import okio.Buffer
import okio.ByteString
import okio.Timeout
import java.io.IOException

/**
 * Okio 코루틴에서 `buffered` 함수를 제공합니다.
 */
fun SuspendedSink.buffered(): BufferedSuspendedSink = RealBufferedSuspendedSink(this)

/**
 * [BufferedSuspendedSink]의 실제 구현체로, 내부 버퍼를 사용하여 비동기적으로 데이터를 기록한다.
 *
 * @property sink 데이터를 기록할 실제 [SuspendedSink] 인스턴스
 * @constructor [sink]를 받아 [RealBufferedSuspendedSink] 인스턴스를 생성한다.
 */
internal class RealBufferedSuspendedSink(private val sink: SuspendedSink): BufferedSuspendedSink {

    companion object: KLoggingChannel()

    override val buffer: Buffer = Buffer()

    private val closed = atomic(false)

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(byteString: ByteString): BufferedSuspendedSink = emitCompleteSegments {
        buffer.write(byteString)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(
        source: ByteArray,
        offset: Int,
        byteCount: Int,
    ): BufferedSuspendedSink = emitCompleteSegments {
        buffer.write(source, offset, byteCount)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(
        source: SuspendedSource,
        byteCount: Long,
    ): BufferedSuspendedSink = apply {
        checkNotClosed()
        var remaining = byteCount
        var noProgressCount = 0
        while (remaining > 0L) {
            val read = source.read(buffer, remaining)
            if (read == -1L) throw okio.EOFException()
            if (read == 0L) {
                noProgressCount++
                if (noProgressCount >= SuspendedSource.MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to write from SuspendedSource: no progress.")
                }
                continue
            }
            noProgressCount = 0
            remaining -= read
            emitCompleteSegments()
        }
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        emitCompleteSegments {
            buffer.write(source, byteCount)
        }
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeAll` 함수를 제공합니다.
     */
    override suspend fun writeAll(source: SuspendedSource): Long {
        checkNotClosed()
        var totalBytesRead = 0L
        var noProgressCount = 0
        while (true) {
            val readCount = source.read(buffer, SEGMENT_SIZE)
            if (readCount == -1L) break
            if (readCount == 0L) {
                noProgressCount++
                if (noProgressCount >= SuspendedSource.MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to writeAll from SuspendedSource: no progress.")
                }
                continue
            }
            noProgressCount = 0
            totalBytesRead += readCount
            emitCompleteSegments()
        }
        return totalBytesRead
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeUtf8` 함수를 제공합니다.
     */
    override suspend fun writeUtf8(
        string: String,
        beginIndex: Int,
        endIndex: Int,
    ): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeUtf8(string, beginIndex, endIndex)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeUtf8CodePoint` 함수를 제공합니다.
     */
    override suspend fun writeUtf8CodePoint(codePoint: Int): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeUtf8CodePoint(codePoint)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeByte` 함수를 제공합니다.
     */
    override suspend fun writeByte(b: Int): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeByte(b)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeShort` 함수를 제공합니다.
     */
    override suspend fun writeShort(s: Int): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeShort(s)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeShortLe` 함수를 제공합니다.
     */
    override suspend fun writeShortLe(s: Int): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeShortLe(s)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeInt` 함수를 제공합니다.
     */
    override suspend fun writeInt(i: Int): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeInt(i)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeIntLe` 함수를 제공합니다.
     */
    override suspend fun writeIntLe(i: Int): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeIntLe(i)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeLong` 함수를 제공합니다.
     */
    override suspend fun writeLong(v: Long): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeLong(v)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeLongLe` 함수를 제공합니다.
     */
    override suspend fun writeLongLe(v: Long): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeLongLe(v)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeDecimalLong` 함수를 제공합니다.
     */
    override suspend fun writeDecimalLong(v: Long): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeDecimalLong(v)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeHexadecimalUnsignedLong` 함수를 제공합니다.
     */
    override suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedSuspendedSink = emitCompleteSegments {
        buffer.writeHexadecimalUnsignedLong(v)
    }

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override suspend fun flush() {
        checkNotClosed()
        if (buffer.size > 0L) {
            sink.write(buffer, buffer.size)
        }
        sink.flush()
    }

    /**
     * Okio 코루틴에서 `emit` 함수를 제공합니다.
     */
    override suspend fun emit(): BufferedSuspendedSink = apply {
        checkNotClosed()
        if (buffer.size > 0L) {
            sink.write(buffer, buffer.size)
        }
    }

    /**
     * Okio 코루틴에서 `emitCompleteSegments` 함수를 제공합니다.
     */
    override suspend fun emitCompleteSegments(): BufferedSuspendedSink = emitCompleteSegments {
        // Noting to do
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override suspend fun close() {
        if (closed.value) {
            return
        }

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
            thrown = thrown ?: e
        }

        closed.value = true

        if (thrown != null) {
            throw thrown
        }
    }

    /**
     * Okio 코루틴에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = sink.timeout()

    private suspend inline fun emitCompleteSegments(block: suspend () -> Unit): BufferedSuspendedSink = apply {
        checkNotClosed()
        block()
        val byteCount = buffer.completeSegmentByteCount()
        if (byteCount > 0L) {
            sink.write(buffer, byteCount)
        }
    }

    private fun checkNotClosed() {
        check(!closed.value) { "RealBufferedSuspendedSink is already closed" }
    }

    /**
     * Okio 코루틴 타입 변환을 위한 `toString` 함수를 제공합니다.
     */
    override fun toString(): String {
        return "RealBufferedSuspendedSink(sink=$sink)"
    }
}
