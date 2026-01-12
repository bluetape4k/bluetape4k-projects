package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source

/**
 * 이 [Buffer]의 모든 바이트를 제거하고 `sink`에 추가합니다. `sink`에 쓰여진 총 바이트 수를 반환합니다.
 * 만약 이 [Buffer]가 소진된 경우 0이 반환됩니다.
 *
 * @param sink 바이트를 쓸 [AsyncSink]
 * @return 쓰여진 바이트 수, `source`가 소진된 경우 0
 */
suspend fun Buffer.suspendReadAll(sink: SuspendedSink): Long {
    val size = this.size
    sink.write(this, size)  // Buffer의 모든 바이트를 sink에 쓰기
    return size
}

/**
 * 이 [BufferedSource]의 모든 바이트를 제거하고 `sink`에 추가합니다. `sink`에 쓰여진 총 바이트 수를 반환합니다.
 * 만약 이 [BufferedSource]가 소진된 경우 0이 반환됩니다.
 *
 * @param sink 바이트를 쓸 [AsyncSink]
 * @return 쓰여진 바이트 수, `source`가 소진된 경우 0
 */
suspend fun BufferedSource.suspendReadAll(sink: SuspendedSink): Long {
    var totalBytesWritten = 0L
    while (read(this.buffer, SEGMENT_SIZE) != -1L) {
        val emitByteCount = buffer.completeSegmentByteCount()
        if (emitByteCount > 0L) {
            totalBytesWritten += emitByteCount
            sink.write(this.buffer, emitByteCount)
        }
    }
    if (buffer.size > 0L) {
        totalBytesWritten += this.buffer.size
        sink.write(this.buffer, this.buffer.size)
    }
    return totalBytesWritten
}

/**
 * [source]로 부터 모든 바이트를 읽어 [BufferedSink] 에 씁니다. 읽은 바이트 수를 반환합니다.
 * `source`가 소진된 경우 0이 반환됩니다.
 *
 * @param source 읽을 [AsyncSource]
 * @return 읽은 바이트 수, `source`가 소진된 경우 0
 */
suspend fun BufferedSink.suspendWriteAll(source: SuspendedSource): Long {
    var totalBytesRead = 0L
    while (true) {
        val readCount = source.read(this.buffer, SEGMENT_SIZE)
        if (readCount == -1L) break
        totalBytesRead += readCount
        emitCompleteSegments()
    }
    return totalBytesRead
}

/**
 * 이 소스에서 모든 바이트를 읽어 [sink]에 씁니다. `sink`에 쓰여진 총 바이트 수를 반환합니다.
 *
 * @param sink 바이트를 쓸 [Sink]
 * @return 쓰여진 바이트 수, `source`가 소진된 경우 0
 */
suspend fun BufferedSuspendedSource.suspendReadAll(sink: Sink): Long {
    var totalBytesWritten = 0L
    while (read(this.buffer, SEGMENT_SIZE) != -1L) {
        val emitByteCount = buffer.completeSegmentByteCount()
        if (emitByteCount > 0L) {
            totalBytesWritten += emitByteCount
            sink.write(this.buffer, emitByteCount)
        }
    }
    if (buffer.size > 0L) {
        totalBytesWritten += buffer.size
        sink.write(this.buffer, buffer.size)
    }
    return totalBytesWritten
}

/**
 * `source`로부터 모든 바이트를 읽어 이 sink에 추가합니다. `source`가 소진된 경우 0이 반환됩니다.
 */
suspend fun BufferedSuspendedSink.suspendWriteAll(source: Source): Long {
    var totalBytesRead = 0L
    while (true) {
        val readCount = source.read(this.buffer, SEGMENT_SIZE)
        if (readCount == -1L) break
        totalBytesRead += readCount
        emitCompleteSegments()
    }
    return totalBytesRead
}

/**
 * `source`로 부터 `byteCount` 바이트를 읽어 이 sink에 추가합니다.
 */
suspend fun BufferedSuspendedSink.suspendWrite(source: Source, byteCount: Long): BufferedSuspendedSink {
    byteCount.requireZeroOrPositiveNumber("byteCount")
    var remaining = byteCount
    while (remaining > 0L) {
        val read = source.read(this.buffer, remaining)
        if (read == -1L) throw okio.EOFException()
        remaining -= read
        emitCompleteSegments()
    }
    return this
}
