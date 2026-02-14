package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspendedSink` 함수를 제공합니다.
 */
fun AsynchronousFileChannel.asSuspendedSink(): SuspendedFileChannelSink =
    SuspendedFileChannelSink(this)

/**
 * [AsynchronousFileChannel] 기반의 [SuspendedSink] 구현체.
 *
 * 쓰기 경로에서 direct [ByteBuffer]를 재사용해 할당/복사 비용을 줄인다.
 * `force()`와 `close()`는 블로킹 가능 API이므로 `Dispatchers.IO`에서 수행한다.
 */
class SuspendedFileChannelSink(
    private val channel: AsynchronousFileChannel,
): SuspendedSink {

    companion object: KLoggingChannel()

    private var position = 0L

    // Reuse one direct buffer to minimize chunk allocation/copy overhead.
    private val writeBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        if (!channel.isOpen) error("Channel is closed")
        require(byteCount <= source.size) { "byteCount[$byteCount] > source.size[${source.size}]" }
        timeout().throwIfReached()

        var remaining = byteCount
        while (remaining > 0L) {
            writeBuffer.clear()
            writeBuffer.limit(minOf(remaining, writeBuffer.capacity().toLong()).toInt())

            // Read directly from Okio Buffer into ByteBuffer to avoid readByteArray() copies.
            val chunkSize = source.read(writeBuffer)
            if (chunkSize <= 0) {
                throw IOException("Unable to read source bytes for AsynchronousFileChannel write.")
            }

            writeBuffer.flip()
            var chunkOffset = 0L
            while (writeBuffer.hasRemaining()) {
                val byteWritten = channel.write(writeBuffer, position + chunkOffset).suspendAwait()
                if (byteWritten <= 0) {
                    throw IOException("Unable to write to AsynchronousFileChannel: no progress.")
                }
                log.debug { "채널 ${position + chunkOffset} 위치에 $byteWritten bytes 를 썼습니다. " }
                chunkOffset += byteWritten
            }

            position += chunkSize.toLong()
            remaining -= chunkSize.toLong()
        }
    }

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override suspend fun flush() {
        withContext(Dispatchers.IO) {
            // force() is synchronous and may block.
            channel.force(false)
        }
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override suspend fun close() {
        withContext(Dispatchers.IO) {
            // Closing file descriptors may block on underlying OS resources.
            if (channel.isOpen) {
                log.debug { "Closing AsynchronousFileChannel[$channel]" }
                channel.close()
            }
        }
    }
}
