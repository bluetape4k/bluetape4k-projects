package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel

fun AsynchronousFileChannel.asSuspendedSink(): SuspendedFileChannelSink =
    SuspendedFileChannelSink(this)

class SuspendedFileChannelSink(
    private val channel: AsynchronousFileChannel,
): SuspendedSink {

    companion object: KLoggingChannel()

    private var position = 0L

    override suspend fun write(source: Buffer, byteCount: Long) {
        if (!channel.isOpen) error("Channel is closed")
        if (byteCount <= 0L) return
        timeout().throwIfReached()

        var remaining = minOf(source.size, byteCount)
        while (remaining > 0L) {
            val chunkSize = minOf(remaining, DEFAULT_BUFFER_SIZE.toLong())
            val byteBuffer = ByteBuffer.wrap(source.readByteArray(chunkSize))
            val byteWritten = channel.write(byteBuffer, position).suspendAwait()
            log.debug { "채널 $position 위치에 $byteWritten bytes 를 썼습니다. " }

            if (byteWritten <= 0) break
            position += byteWritten
            remaining -= byteWritten
        }
    }

    override suspend fun flush() = coroutineScope {
        // Cannot alter meta data through this sink
        channel.force(false)
    }

    override suspend fun close() = coroutineScope {
        if (channel.isOpen) {
            log.debug { "Closing AsynchronousFileChannel[$channel]" }
            channel.close()
        }
    }
}
