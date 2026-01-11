package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

fun ReadableByteChannel.asSource(timeout: Timeout = Timeout.NONE): ByteChannelSource =
    ByteChannelSource(this, timeout)

class ByteChannelSource(
    private val channel: ReadableByteChannel,
    private val timeout: Timeout = Timeout.NONE,
): Source {

    companion object: KLogging()

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (!channel.isOpen) error("Channel[$channel] is closed")
        if (byteCount <= 0L) return -1L

        Buffer.UnsafeCursor().use { cursor ->
            sink.readAndWriteUnsafe(cursor).use { ignored ->
                timeout.throwIfReached()
                val oldSize = sink.size
                val length = byteCount.toInt()

                cursor.expandBuffer(length)
                val read = channel.read(ByteBuffer.wrap(cursor.data, cursor.start, length))
                log.debug { "채널의 ${cursor.start} 위치에서 $read 바이트를 읽었습니다." }

                return when (read) {
                    -1 -> {
                        cursor.resizeBuffer(oldSize)
                        -1L
                    }
                    else -> {
                        cursor.resizeBuffer(oldSize + read)
                        read.toLong()
                    }
                }
            }
        }
    }

    fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        while (true) {
            val bytesToRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesToRead == -1L) break
            totalBytesRead += bytesToRead
        }
        return totalBytesRead
    }

    override fun timeout(): Timeout = timeout
    override fun close() {
        channel.close()
    }
}
