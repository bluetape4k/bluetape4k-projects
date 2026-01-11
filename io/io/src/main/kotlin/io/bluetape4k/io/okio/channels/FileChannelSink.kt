package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.nio.channels.FileChannel

fun FileChannel.asSink(timeout: Timeout = Timeout.NONE) =
    FileChannelSink(this, timeout)

class FileChannelSink(
    private val channel: FileChannel,
    private val timeout: Timeout = Timeout.NONE,
): Sink {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        if (!channel.isOpen) error("Channel[$channel] is closed")
        if (byteCount <= 0L) return
        timeout.throwIfReached()

        var remaining = byteCount
        while (remaining > 0L) {
            val written = channel.transferFrom(source, channel.position(), remaining)
            log.debug { "채널의 ${channel.position()} 위치에 $written 바이트를 썼습니다." }
            channel.position(channel.position() + written)
            remaining -= written
        }
    }

    override fun flush() {
        // Cannot alter meta data through this sink 
        channel.force(false)
    }

    override fun timeout(): Timeout = timeout

    override fun close() {
        channel.close()
    }
}
