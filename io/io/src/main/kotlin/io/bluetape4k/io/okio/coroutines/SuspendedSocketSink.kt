package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.io.okio.readUnsafeAndClose
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

/**
 * Okio 코루틴 타입 변환을 위한 `asSuspendedSink` 함수를 제공합니다.
 */
fun Socket.asSuspendedSink(): SuspendedSocketSink = SuspendedSocketSink(this)

/**
 * non-blocking [SocketChannel]을 이용해 소켓 쓰기를 제공하는 [SuspendedSink].
 *
 * Selector 기반으로 쓰기 가능 시점을 기다리며, close는 `Dispatchers.IO`에서 수행한다.
 */
class SuspendedSocketSink(socket: Socket): SuspendedSink {

    companion object: KLoggingChannel()

    private val channel: SocketChannel = requireNotNull(socket.channel) {
        "Socket.channel is null. Use SocketChannel.open() or SocketChannel.socket()."
    }.apply {
        configureBlocking(false)
    }

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        require(byteCount <= source.size) { "byteCount[$byteCount] > source.size[${source.size}]" }

        channel.await(SelectionKey.OP_WRITE)

        source.readUnsafeAndClose { cursor ->
            var remaining = byteCount
            while (remaining > 0) {
                cursor.seek(0)
                val safeRemaining = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                val length = minOf(cursor.end - cursor.start, safeRemaining)
                val written = channel.write(ByteBuffer.wrap(cursor.data, cursor.start, length))

                when {
                    written > 0 -> {
                        remaining -= written.toLong()
                        source.skip(written.toLong())
                    }
                    written == 0 -> channel.await(SelectionKey.OP_WRITE)
                    else -> throw IOException("channel closed")
                }
            }
        }
    }

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override suspend fun flush() {
        // Nothing to do
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
