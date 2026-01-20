package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.readUnsafeAndClose
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

fun AsynchronousSocketChannel.asSuspendedSink(): SuspendedSocketChannelSink =
    SuspendedSocketChannelSink(this)

class SuspendedSocketChannelSink(
    private val channel: AsynchronousSocketChannel,
): SuspendedSink {

    companion object: KLoggingChannel()

    private val cursor = Buffer.UnsafeCursor()

    override suspend fun write(source: Buffer, byteCount: Long) {
        source.readUnsafeAndClose(cursor) { cursor ->
            val byteBuffer = ByteBuffer.allocate(byteCount.toInt())
            val offset = if (cursor.start < 0) 0 else cursor.start
            byteBuffer.put(cursor.data, offset, byteCount.toInt())
            channel.write(byteBuffer)
        }
    }

    override suspend fun flush() {
        // Nothing to do here
    }

    override suspend fun close() = coroutineScope {
        if (channel.isOpen) {
            log.debug { "Closing socket channel[$channel]" }
            channel.close()
        }
    }
}
