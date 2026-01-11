package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.Source
import okio.Timeout
import java.nio.channels.FileChannel

fun FileChannel.asSource(timeout: Timeout = Timeout.NONE) =
    FileChannelSource(this, timeout)

class FileChannelSource(
    private val channel: FileChannel,
    private val timeout: Timeout = Timeout.NONE,
): Source {
    companion object: KLogging()

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (!channel.isOpen) error("Channel[$channel] is closed")
        if (byteCount <= 0L) return -1L
        if (channel.position() == channel.size()) return -1L
        timeout.throwIfReached()

        val read = channel.transferTo(channel.position(), byteCount, sink)
        log.debug { "채널의 ${channel.position()} 위치에서 $read 바이트를 읽었습니다." }

        channel.position(channel.position() + read)
        return read
    }

    fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        while (true) {
            val bytesRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead <= 0L) break
            totalBytesRead += bytesRead
        }
        return totalBytesRead
    }

    override fun timeout(): Timeout = timeout

    override fun close() {
        channel.close()
    }
}
