package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * Okio 채널 I/O 타입 변환을 위한 `asSink` 함수를 제공합니다.
 */
fun FileChannel.asSink(timeout: Timeout = Timeout.NONE) =
    FileChannelSink(this, timeout)

/**
 * Okio 채널 I/O에서 사용하는 `FileChannelSink` 타입입니다.
 */
class FileChannelSink(
    private val channel: FileChannel,
    private val timeout: Timeout = Timeout.NONE,
): Sink {

    companion object: KLogging()

    /**
     * Okio 채널 I/O에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")
        if (!channel.isOpen) error("Channel[$channel] is closed")

        timeout.throwIfReached()

        var remaining = byteCount
        while (remaining > 0L) {
            val written = channel.transferFrom(source, channel.position(), remaining)
            log.debug { "채널의 ${channel.position()} 위치에 $written 바이트를 썼습니다." }
            if (written <= 0L) {
                throw IOException("No bytes were written to file channel[$channel]")
            }
            channel.position(channel.position() + written)
            remaining -= written
        }
    }

    /**
     * Okio 채널 I/O 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override fun flush() {
        // Cannot alter meta data through this sink 
        channel.force(false)
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
