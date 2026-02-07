package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.io.okio.readUnsafeAndClose
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import java.io.IOException
import java.net.Socket
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

fun Socket.asSuspendedSink(): SuspendedSocketSink = SuspendedSocketSink(this)

class SuspendedSocketSink(socket: Socket): SuspendedSink {

    companion object: KLoggingChannel()

    private val channel: SocketChannel = requireNotNull(socket.channel) {
        "Socket.channel is null. Use SocketChannel.open() or SocketChannel.socket()."
    }.apply {
        configureBlocking(false)
    }

    override suspend fun write(source: Buffer, byteCount: Long) {
        byteCount.requireZeroOrPositiveNumber("byteCount")

        channel.await(SelectionKey.OP_WRITE)
        if (byteCount == 0L) return

        source.readUnsafeAndClose { cursor ->
            var remaining = byteCount
            while (remaining > 0) {
                cursor.seek(0)
                val length = minOf(cursor.end - cursor.start, remaining.toInt())
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

    override suspend fun flush() {
        // Nothing to do
    }

    override suspend fun close() = coroutineScope {
        if (channel.isOpen) {
            log.debug { "Closing socket channel[$channel]" }
            channel.close()
        }
    }
}
