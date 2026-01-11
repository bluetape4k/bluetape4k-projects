package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

fun WritableByteChannel.asSink(timeout: Timeout = Timeout.NONE): ByteChannelSink =
    ByteChannelSink(this, timeout)

class ByteChannelSink(
    private val channel: WritableByteChannel,
    private val timeout: Timeout = Timeout.NONE,
): Sink {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        if (!channel.isOpen) error("Channel[$channel] is closed")
        if (byteCount <= 0L) return

        Buffer.UnsafeCursor().use { cursor ->
            var remaining = byteCount
            while (remaining > 0L) {
                timeout.throwIfReached()
                source.readUnsafe(cursor).use { ignored ->
                    cursor.seek(0L)
                    val length = minOf(cursor.end - cursor.start, remaining.toInt())
                    val written = channel.write(ByteBuffer.wrap(cursor.data, cursor.start, length))
                    log.debug { "채널의 ${cursor.start} 위치에 $written 바이트를 썼습니다." }
                    remaining -= written.toLong()
                    source.skip(written.toLong())
                }
            }
        }
    }

    override fun flush() {
        // Nothing to do
    }

    override fun timeout(): Timeout = timeout

    override fun close() {
        channel.close()
    }
}
