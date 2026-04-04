package io.bluetape4k.okio.coroutines

import io.bluetape4k.okio.SEGMENT_SIZE
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source
import java.io.IOException

private const val MAX_NO_PROGRESS_READS = 8

/**
 * 이 [Buffer]의 모든 바이트를 제거하고 [sink]에 추가합니다. [sink]에 쓰여진 총 바이트 수를 반환합니다.
 * 만약 이 [Buffer]가 소진된 경우 0이 반환됩니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello")
 * val suspendedSink = Buffer().asSuspended()
 * val written = buffer.suspendReadAll(suspendedSink)
 * // written == 5L
 * ```
 *
 * @param sink 바이트를 쓸 [SuspendedSink]
 * @return 쓰여진 바이트 수, `source`가 소진된 경우 0
 */
suspend fun Buffer.suspendReadAll(sink: SuspendedSink): Long {
    val size = this.size
    sink.write(this, size)  // Buffer의 모든 바이트를 sink에 쓰기
    return size
}

/**
 * 이 [BufferedSource]의 모든 바이트를 제거하고 [sink]에 추가합니다. [sink]에 쓰여진 총 바이트 수를 반환합니다.
 * 만약 이 [BufferedSource]가 소진된 경우 0이 반환됩니다.
 *
 * ```kotlin
 * val source = bufferOf("hello world")
 * val output = Buffer()
 * val suspendedSink = output.asSuspended()
 * val written = source.suspendReadAll(suspendedSink)
 * // written == 11L
 * ```
 *
 * @param sink 바이트를 쓸 [SuspendedSink]
 * @return 쓰여진 바이트 수, `source`가 소진된 경우 0
 */
suspend fun BufferedSource.suspendReadAll(sink: SuspendedSink): Long {
    var totalBytesWritten = 0L
    var noProgressCount = 0
    val tempBuffer = Buffer()
    while (true) {
        val readCount = read(tempBuffer, SEGMENT_SIZE)
        if (readCount == -1L) break
        if (readCount == 0L) {
            noProgressCount++
            if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                throw IOException("Unable to suspendReadAll from BufferedSource: no progress.")
            }
            continue
        }
        noProgressCount = 0

        val emitByteCount = tempBuffer.completeSegmentByteCount()
        if (emitByteCount > 0L) {
            totalBytesWritten += emitByteCount
            sink.write(tempBuffer, emitByteCount)
        }
    }
    if (tempBuffer.size > 0L) {
        totalBytesWritten += tempBuffer.size
        sink.write(tempBuffer, tempBuffer.size)
    }
    return totalBytesWritten
}

/**
 * [source]로부터 모든 바이트를 읽어 이 [BufferedSink]에 씁니다. 읽은 바이트 수를 반환합니다.
 * `source`가 소진된 경우 0이 반환됩니다.
 *
 * ```kotlin
 * val suspendedSource = bufferOf("hello").asSuspended()
 * val output = Buffer()
 * val bufferedSink = (output as okio.Sink).buffered()
 * val read = bufferedSink.suspendWriteAll(suspendedSource)
 * // read == 5L
 * ```
 *
 * @param source 읽을 [SuspendedSource]
 * @return 읽은 바이트 수, `source`가 소진된 경우 0
 */
suspend fun BufferedSink.suspendWriteAll(source: SuspendedSource): Long {
    var totalBytesRead = 0L
    var noProgressCount = 0
    while (true) {
        val readCount = source.read(this.buffer, SEGMENT_SIZE)
        if (readCount == -1L) break
        if (readCount == 0L) {
            noProgressCount++
            if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                throw IOException("Unable to suspendWriteAll from SuspendedSource: no progress.")
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
 * 이 [BufferedSuspendedSource]에서 모든 바이트를 읽어 블로킹 [sink]에 씁니다.
 * [sink]에 쓰여진 총 바이트 수를 반환합니다.
 *
 * ```kotlin
 * val suspendedSource = bufferOf("hello").asSuspended().buffered()
 * val output = Buffer()
 * val written = suspendedSource.suspendReadAll(output as Sink)
 * // written == 5L
 * ```
 *
 * @param sink 바이트를 쓸 [Sink]
 * @return 쓰여진 바이트 수, `source`가 소진된 경우 0
 */
suspend fun BufferedSuspendedSource.suspendReadAll(sink: Sink): Long {
    var totalBytesWritten = 0L
    var noProgressCount = 0
    val tempBuffer = Buffer()
    while (true) {
        val readCount = read(tempBuffer, SEGMENT_SIZE)
        if (readCount == -1L) break
        if (readCount == 0L) {
            noProgressCount++
            if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                throw IOException("Unable to suspendReadAll from BufferedSuspendedSource: no progress.")
            }
            continue
        }
        noProgressCount = 0

        val emitByteCount = tempBuffer.completeSegmentByteCount()
        if (emitByteCount > 0L) {
            totalBytesWritten += emitByteCount
            sink.write(tempBuffer, emitByteCount)
        }
    }
    if (tempBuffer.size > 0L) {
        totalBytesWritten += tempBuffer.size
        sink.write(tempBuffer, tempBuffer.size)
    }
    return totalBytesWritten
}

/**
 * 블로킹 [source]로부터 모든 바이트를 읽어 이 [BufferedSuspendedSink]에 씁니다.
 * `source`가 소진된 경우 0이 반환됩니다.
 *
 * ```kotlin
 * val suspendedSink = Buffer().asSuspended().buffered()
 * val source = bufferOf("hello")
 * val read = suspendedSink.suspendWriteAll(source as Source)
 * // read == 5L
 * ```
 *
 * @param source 읽을 블로킹 [Source]
 * @return 읽은 총 바이트 수
 */
suspend fun BufferedSuspendedSink.suspendWriteAll(source: Source): Long {
    var totalBytesRead = 0L
    var noProgressCount = 0
    while (true) {
        val readCount = source.read(this.buffer, SEGMENT_SIZE)
        if (readCount == -1L) break
        if (readCount == 0L) {
            noProgressCount++
            if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                throw IOException("Unable to suspendWriteAll from Source: no progress.")
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
 * 블로킹 [source]로부터 정확히 [byteCount] 바이트를 읽어 이 [BufferedSuspendedSink]에 씁니다.
 * 소스가 소진되기 전에 [byteCount] 바이트를 읽지 못하면 [okio.EOFException]을 던집니다.
 *
 * ```kotlin
 * val suspendedSink = Buffer().asSuspended().buffered()
 * val source = bufferOf("hello world")
 * suspendedSink.suspendWrite(source as Source, 5L)
 * // 5바이트 "hello"가 sink에 기록됨
 * ```
 *
 * @param source 읽을 블로킹 [Source]
 * @param byteCount 읽을 바이트 수
 * @return 이 [BufferedSuspendedSink] 인스턴스 (체이닝용)
 * @throws okio.EOFException 소스가 소진된 경우
 */
suspend fun BufferedSuspendedSink.suspendWrite(source: Source, byteCount: Long): BufferedSuspendedSink {
    if (byteCount <= 0L) return this
    var remaining = byteCount
    var noProgressCount = 0
    while (remaining > 0L) {
        val read = source.read(this.buffer, remaining)
        if (read == -1L) throw okio.EOFException()
        if (read == 0L) {
            noProgressCount++
            if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                throw IOException("Unable to suspendWrite from Source: no progress.")
            }
            continue
        }
        noProgressCount = 0
        remaining -= read
        emitCompleteSegments()
    }
    return this
}
