package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.io.okio.readUnsafeAndClose
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

fun Socket.asSuspendedSink(): SuspendedSocketSink = SuspendedSocketSink(this)

class SuspendedSocketSink(socket: Socket): SuspendedSink {

    companion object: KLoggingChannel()

    private val channel = socket.channel

    override suspend fun write(source: Buffer, byteCount: Long) {
        byteCount.requireZeroOrPositiveNumber("byteCount")

        channel.await(SelectionKey.OP_WRITE)
        source.readUnsafeAndClose { cursor ->
            var remaining = byteCount
            while (remaining > 0) {
                cursor.seek(0)
                val length = minOf(cursor.end - cursor.start, remaining.toInt())
                val written = channel.write(ByteBuffer.wrap(cursor.data, cursor.start, length))

                if (written <= 0) {
                    channel.await(SelectionKey.OP_WRITE)
                }
                remaining -= written.toLong()
                source.skip(written.toLong())
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
