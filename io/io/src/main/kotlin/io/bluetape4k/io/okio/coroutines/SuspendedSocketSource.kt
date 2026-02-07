package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.SEGMENT_SIZE
import io.bluetape4k.io.okio.coroutines.internal.await
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import java.net.Socket
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

fun Socket.asSuspendedSource(): SuspendedSocketSource = SuspendedSocketSource(this)

class SuspendedSocketSource(socket: Socket): SuspendedSource {

    companion object: KLoggingChannel()

    private val channel: SocketChannel = requireNotNull(socket.channel) {
        "Socket.channel is null. Use SocketChannel.open() or SocketChannel.socket()."
    }.apply {
        configureBlocking(false)
    }
    private val byteBuffer = ByteBuffer.allocateDirect(SEGMENT_SIZE.toInt())

    override suspend fun read(sink: Buffer, byteCount: Long): Long = coroutineScope {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return@coroutineScope 0L

        channel.await(SelectionKey.OP_READ)

        byteBuffer.clear()
        byteBuffer.limit(minOf(SEGMENT_SIZE, byteCount).toInt())

        val read = channel.read(byteBuffer)
        byteBuffer.flip()

        if (read > 0) {
            sink.write(byteBuffer)
        } else if (read < 0) {
            return@coroutineScope -1L
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
