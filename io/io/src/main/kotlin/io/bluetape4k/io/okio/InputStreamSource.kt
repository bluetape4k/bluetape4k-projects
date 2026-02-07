package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.Source
import okio.Timeout
import java.io.InputStream

fun InputStream.asSource(timeout: Timeout = Timeout.NONE) =
    InputStreamSource(this, timeout)

class InputStreamSource(
    private val input: InputStream,
    private val timeout: Timeout = Timeout.NONE,
): Source {

    companion object: KLogging()

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (byteCount == 0L) return 0L

        Buffer.UnsafeCursor().use { cursor ->
            sink.readAndWriteUnsafe(cursor).use { ignored ->
                timeout.throwIfReached()

                val originalSize = sink.size
                val length = byteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

                cursor.expandBuffer(length)
                val read = input.read(cursor.data, cursor.start, length)
                log.debug { "InputStream의 ${cursor.start} 위치로부터 $read bytes 를 읽었습니다." }

                return if (read > 0) {
                    cursor.resizeBuffer(originalSize + read)
                    read.toLong()
                } else {
                    cursor.resizeBuffer(originalSize)
                    -1L
                }
            }
        }
    }

    fun readAll(sink: Buffer): Long {
        var totalReadCount = 0L
        while (true) {
            val read = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (read <= 0L) break
            totalReadCount += read
        }
        return totalReadCount
    }

    override fun timeout(): Timeout = timeout

    override fun close() {
        input.close()
    }
}
