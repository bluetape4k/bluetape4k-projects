package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.Source
import okio.Timeout
import java.nio.channels.FileChannel

/**
 * Okio 채널 I/O 타입 변환을 위한 `asSource` 함수를 제공합니다.
 */
fun FileChannel.asSource(timeout: Timeout = Timeout.NONE) =
    FileChannelSource(this, timeout)

/**
 * Okio 채널 I/O에서 사용하는 `FileChannelSource` 타입입니다.
 */
class FileChannelSource(
    private val channel: FileChannel,
    private val timeout: Timeout = Timeout.NONE,
): Source {
    companion object: KLogging()

    /**
     * Okio 채널 I/O에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L
        if (!channel.isOpen) error("Channel[$channel] is closed")
        if (channel.position() == channel.size()) return -1L
        timeout.throwIfReached()

        val read = channel.transferTo(channel.position(), byteCount, sink)
        log.debug { "채널의 ${channel.position()} 위치에서 $read 바이트를 읽었습니다." }

        channel.position(channel.position() + read)
        return read
    }

    /**
     * Okio 채널 I/O에서 데이터를 읽어오는 `readAll` 함수를 제공합니다.
     */
    fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        while (true) {
            val bytesRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead <= 0L) break
            totalBytesRead += bytesRead
        }
        return totalBytesRead
    }

    /**
     * Okio 채널 I/O에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = timeout

    /**
     * Okio 채널 I/O 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        channel.close()
    }
}
