package io.bluetape4k.io.okio.channels

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

/**
 * Okio 채널 I/O 타입 변환을 위한 `asSource` 함수를 제공합니다.
 */
fun ReadableByteChannel.asSource(timeout: Timeout = Timeout.NONE): ByteChannelSource =
    ByteChannelSource(this, timeout)

/**
 * Okio 채널 I/O에서 사용하는 `ByteChannelSource` 타입입니다.
 */
class ByteChannelSource(
    private val channel: ReadableByteChannel,
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

        sink.readAndWriteUnsafe(Buffer.UnsafeCursor()).use { cursor ->
            timeout.throwIfReached()
            val oldSize = sink.size
            val length = byteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            cursor.expandBuffer(length)
            val read = channel.read(ByteBuffer.wrap(cursor.data, cursor.start, length))
            log.debug { "채널의 ${cursor.start} 위치에서 $read 바이트를 읽었습니다." }

            return when {
                read > 0   -> {
                    cursor.resizeBuffer(oldSize + read)
                    read.toLong()
                }
                read == -1 -> {
                    cursor.resizeBuffer(oldSize)
                    -1L
                }
                else       -> {
                    cursor.resizeBuffer(oldSize)
                    0L
                }
            }
        }
    }

    /**
     * Okio 채널 I/O에서 데이터를 읽어오는 `readAll` 함수를 제공합니다.
     */
    fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        while (true) {
            val bytesToRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesToRead <= 0L) break
            totalBytesRead += bytesToRead
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
