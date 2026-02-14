package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspendedSource` 함수를 제공합니다.
 */
fun AsynchronousSocketChannel.asSuspendedSource(): SuspendedSocketChannelSource =
    SuspendedSocketChannelSource(this)

/**
 * [AsynchronousSocketChannel] 기반의 [SuspendedSource] 구현체.
 *
 * 반복 read에서 direct [ByteBuffer]를 재사용해 할당 비용을 줄이고,
 * close는 블로킹 가능성을 고려해 `Dispatchers.IO`에서 수행한다.
 */
class SuspendedSocketChannelSource(
    private val channel: AsynchronousSocketChannel,
): SuspendedSource {

    companion object: KLoggingChannel()

    // Keep one direct buffer for repeated async channel reads.
    private val byteBuffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    /**
     * Okio 코루틴에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override suspend fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L
        if (!channel.isOpen) return -1L

        byteBuffer.clear()
        byteBuffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())

        val read = channel.read(byteBuffer).suspendAwait()
        if (read <= 0) return read.toLong()
        byteBuffer.flip()

        sink.write(byteBuffer)
        return read.toLong()
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
