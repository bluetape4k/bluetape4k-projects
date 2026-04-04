package io.bluetape4k.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

/**
 * [WritableByteChannel]을 Okio [Sink]로 변환합니다.
 *
 * ```kotlin
 * val channel = java.nio.channels.Channels.newChannel(java.io.ByteArrayOutputStream())
 * val sink = channel.asSink()
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * ```
 */
fun WritableByteChannel.asSink(timeout: Timeout = Timeout.NONE): ByteChannelSink = ByteChannelSink(this, timeout)

/**
 * [WritableByteChannel]을 Okio [Sink]로 감싼 구현체입니다.
 *
 * ```kotlin
 * val baos = java.io.ByteArrayOutputStream()
 * val channel = java.nio.channels.Channels.newChannel(baos)
 * val sink = ByteChannelSink(channel)
 * val source = bufferOf("hello")
 * sink.write(source, source.size)
 * sink.close()
 * val text = baos.toString(Charsets.UTF_8)
 * // text == "hello"
 * ```
 */
class ByteChannelSink(
    private val channel: WritableByteChannel,
    private val timeout: Timeout = Timeout.NONE,
) : Sink {
    companion object : KLogging()

    /**
     * Okio 채널 I/O에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(
        source: Buffer,
        byteCount: Long,
    ) {
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")
        if (!channel.isOpen) error("Channel[$channel] is closed")

        val cursor = Buffer.UnsafeCursor()
        var remaining = byteCount
        while (remaining > 0L) {
            timeout.throwIfReached()
            source.readUnsafe(cursor).use { ignored ->
                cursor.seek(0L)
                val maxWriteLength = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                val length = minOf(cursor.end - cursor.start, maxWriteLength)

                val written = channel.write(ByteBuffer.wrap(cursor.data, cursor.start, length))
                log.debug { "채널의 ${cursor.start} 위치에 $written 바이트를 썼습니다." }
                if (written <= 0) {
                    throw IOException("No bytes were written to channel[$channel]")
                }

                remaining -= written.toLong()
                source.skip(written.toLong())
            }
        }
    }

    /**
     * Okio 채널 I/O 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override fun flush() {
        // Nothing to do
    }

    /**
     * Okio 채널 I/O에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = timeout

    /**
     * Okio 채널 I/O 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        runCatching { channel.close() }.onFailure { log.debug(it) { "채널 닫기 실패: $channel" } }
    }
}
