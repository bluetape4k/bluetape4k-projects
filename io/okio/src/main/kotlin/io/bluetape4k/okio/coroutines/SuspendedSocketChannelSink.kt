package io.bluetape4k.okio.coroutines

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.okio.SEGMENT_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

/**
 * [AsynchronousSocketChannel]을 코루틴 방식의 [SuspendedSocketChannelSink]로 변환합니다.
 *
 * ```kotlin
 * val channel = AsynchronousSocketChannel.open()
 * // channel.connect(...) 후 사용
 * val sink = channel.asSuspendedSink()
 * // sink를 코루틴에서 사용 가능
 * ```
 */
fun AsynchronousSocketChannel.asSuspendedSink(): SuspendedSocketChannelSink =
    SuspendedSocketChannelSink(this)

/**
 * [AsynchronousSocketChannel] 기반의 코루틴 [SuspendedSink] 구현체.
 *
 * 쓰기 경로에서 direct [ByteBuffer]를 재사용해 chunk 할당을 줄이며,
 * close는 블로킹 가능성을 고려해 `Dispatchers.IO`에서 수행한다.
 *
 * ```kotlin
 * val channel = AsynchronousSocketChannel.open()
 * // channel.connect(addr).get() 후 사용
 * val sink = SuspendedSocketChannelSink(channel)
 * val source = bufferOf("GET / HTTP/1.1\r\n\r\n")
 * sink.write(source, source.size)
 * sink.flush()
 * sink.close()
 * ```
 */
class SuspendedSocketChannelSink(
    private val channel: AsynchronousSocketChannel,
): SuspendedSink {

    companion object: KLoggingChannel()

    // Reuse one direct buffer to avoid per-chunk ByteArray allocation.
    private val writeBuffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        require(byteCount <= source.size) { "byteCount[$byteCount] > source.size[${source.size}]" }
        if (!channel.isOpen) error("Channel is closed")

        var remaining = byteCount
        while (remaining > 0L) {
            writeBuffer.clear()
            writeBuffer.limit(minOf(remaining, writeBuffer.capacity().toLong()).toInt())

            // Read directly from Okio Buffer into ByteBuffer to avoid readByteArray() copies.
            val read = source.read(writeBuffer)
            if (read <= 0) throw EOFException()

            writeBuffer.flip()
            while (writeBuffer.hasRemaining()) {
                val written = channel.write(writeBuffer).awaitSuspending()
                if (written <= 0) {
                    throw IOException("channel closed")
                }
            }

            remaining -= read.toLong()
        }
    }

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override suspend fun flush() {
        // Nothing to do here
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override suspend fun close() {
        withContext(Dispatchers.IO) {
            // Close can block while native resources are released.
            if (channel.isOpen) {
                log.debug { "Closing socket channel[$channel]" }
                channel.close()
            }
        }
    }
}
