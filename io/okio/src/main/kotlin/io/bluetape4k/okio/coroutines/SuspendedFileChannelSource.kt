package io.bluetape4k.okio.coroutines

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel

/**
 * [AsynchronousFileChannel]을 코루틴 방식의 [SuspendedFileChannelSource]로 변환합니다.
 *
 * ```kotlin
 * val file = kotlin.io.path.createTempFile()
 * file.writeText("hello")
 * val channel = AsynchronousFileChannel.open(file,
 *     java.nio.file.StandardOpenOption.READ)
 * val source = channel.asSuspendedSource()
 * // source를 코루틴에서 사용 가능
 * source.close()
 * ```
 */
fun AsynchronousFileChannel.asSuspendedSource(): SuspendedFileChannelSource =
    SuspendedFileChannelSource(this)

/**
 * [AsynchronousFileChannel] 기반의 코루틴 [SuspendedSource] 구현체.
 *
 * 성능을 위해 단일 direct [ByteBuffer]를 재사용하며, EOF는 `read()` 결과로 판별한다.
 * 리소스 해제는 블로킹 가능성이 있어 `Dispatchers.IO`에서 수행한다.
 *
 * ```kotlin
 * val file = kotlin.io.path.createTempFile()
 * file.writeText("hello")
 * val channel = AsynchronousFileChannel.open(file,
 *     java.nio.file.StandardOpenOption.READ)
 * val source = SuspendedFileChannelSource(channel)
 * val sink = Buffer()
 * source.readAll(sink)
 * val text = sink.readUtf8()
 * // text == "hello"
 * source.close()
 * ```
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

            val bytesRead = channel.read(readBuffer, position).awaitSuspending()

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
