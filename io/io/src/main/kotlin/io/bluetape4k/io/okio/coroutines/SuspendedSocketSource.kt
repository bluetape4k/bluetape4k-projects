package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspendedSource` 함수를 제공합니다.
 */
fun Socket.asSuspendedSource(): SuspendedSocketSource = SuspendedSocketSource(this)

/**
 * non-blocking [SocketChannel]을 이용해 소켓 읽기를 제공하는 [SuspendedSource].
 *
 * Selector 기반으로 읽기 가능 시점을 기다린 뒤 재사용 direct [ByteBuffer]로 읽는다.
 * 소켓 close는 블로킹 가능성이 있어 `Dispatchers.IO`에서 수행한다.
 */
class SuspendedSocketSource(socket: Socket): SuspendedSource {

    companion object: KLoggingChannel()

    private val channel: SocketChannel = requireNotNull(socket.channel) {
        "Socket.channel is null. Use SocketChannel.open() or SocketChannel.socket()."
    }.apply {
        configureBlocking(false)
    }

    // Keep one direct buffer for repeated non-blocking reads.
    private val byteBuffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    /**
     * Okio 코루틴에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override suspend fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L

        while (true) {
            channel.await(SelectionKey.OP_READ)

            byteBuffer.clear()
            byteBuffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())

            val read = channel.read(byteBuffer)
            if (read < 0) {
                return -1L
            }
            if (read == 0) {
                continue
            }

            byteBuffer.flip()
            sink.write(byteBuffer)
            return read.toLong()
        }
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override suspend fun close() {
        withContext(Dispatchers.IO) {
            // Close can block while the socket is being torn down.
            if (channel.isOpen) {
                log.debug { "Closing socket channel[$channel]" }
                channel.close()
            }
        }
    }
}
