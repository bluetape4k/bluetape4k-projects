package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspendedSource` 함수를 제공합니다.
 */
fun AsynchronousFileChannel.asSuspendedSource(): SuspendedFileChannelSource =
    SuspendedFileChannelSource(this)

/**
 * [AsynchronousFileChannel] 기반의 [SuspendedSource] 구현체.
 *
 * 성능을 위해 단일 direct [ByteBuffer]를 재사용하며, EOF는 `read()` 결과로 판별한다.
 * 리소스 해제는 블로킹 가능성이 있어 `Dispatchers.IO`에서 수행한다.
 */
class SuspendedFileChannelSource(
    private val channel: AsynchronousFileChannel,
): SuspendedSource {

    companion object: KLoggingChannel()

    private var position = 0L

    // Reuse one direct buffer to avoid per-read allocation in hot path.
    private val readBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)

    /**
     * Okio 코루틴에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override suspend fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L
        if (!channel.isOpen) error("Channel is closed")
        timeout().throwIfReached()

        var remaining = byteCount
        var totalRead = 0L
        while (remaining > 0L) {
            readBuffer.clear()
            readBuffer.limit(minOf(remaining, readBuffer.capacity().toLong()).toInt())

            val bytesRead = channel.read(readBuffer, position).suspendAwait()

            // Trust read result for EOF/partial read instead of calling channel.size() every time.
            if (bytesRead < 0) {
                return if (totalRead == 0L) -1L else totalRead
            }
            if (bytesRead == 0) break

            readBuffer.flip()
            sink.write(readBuffer)
            log.debug { "채널 $position 위치에서 $bytesRead bytes 를 읽었습니다." }

            position += bytesRead
            totalRead += bytesRead
            remaining -= bytesRead
        }
        return totalRead
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
