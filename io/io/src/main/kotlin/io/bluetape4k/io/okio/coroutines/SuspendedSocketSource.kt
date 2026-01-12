package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

fun Socket.asSuspendedSource(): SuspendedSocketSource = SuspendedSocketSource(this)

class SuspendedSocketSource(socket: Socket): SuspendedSource {

    companion object: KLoggingChannel()

    private val channel = socket.channel
    private val byteBuffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    override suspend fun read(sink: Buffer, byteCount: Long): Long = coroutineScope {
        byteCount.requireZeroOrPositiveNumber("byteCount")

        channel.await(SelectionKey.OP_READ)

        byteBuffer.clear()
        byteBuffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())

        val read = channel.read(byteBuffer)
        byteBuffer.flip()

        if (read > 0) {
            sink.write(byteBuffer)
        }

        return@coroutineScope read.toLong()
    }

    override suspend fun close() = coroutineScope {
        if (channel.isOpen) {
            log.debug { "Closing socket channel[$channel]" }
            channel.close()
        }
    }
}
