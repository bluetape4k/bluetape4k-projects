package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import okio.EOFException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

fun AsynchronousSocketChannel.asSuspendedSink(): SuspendedSocketChannelSink =
    SuspendedSocketChannelSink(this)

class SuspendedSocketChannelSink(
    private val channel: AsynchronousSocketChannel,
): SuspendedSink {

    companion object: KLoggingChannel()

    override suspend fun write(source: Buffer, byteCount: Long) {
        if (byteCount == 0L) return
        if (!channel.isOpen) error("Channel is closed")
        require(byteCount >= 0) { "byteCount must be zero or positive, but was $byteCount" }

        var remaining = byteCount
        while (remaining > 0L) {
            val toRead = minOf(remaining, source.size, SEGMENT_SIZE)
            if (toRead == 0L) throw EOFException()

            val chunk = source.readByteArray(toRead)
            var offset = 0
            while (offset < chunk.size) {
                val written = channel.write(ByteBuffer.wrap(chunk, offset, chunk.size - offset)).suspendAwait()
                if (written <= 0) {
                    throw java.io.IOException("channel closed")
                }
                offset += written
            }

            remaining -= toRead
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
