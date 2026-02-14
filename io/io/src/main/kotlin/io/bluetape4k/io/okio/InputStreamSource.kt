package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.Source
import okio.Timeout
import java.io.InputStream

/**
 * Okio I/O 타입 변환을 위한 `asSource` 함수를 제공합니다.
 */
fun InputStream.asSource(timeout: Timeout = Timeout.NONE) =
    InputStreamSource(this, timeout)

/**
 * Okio I/O에서 사용하는 `InputStreamSource` 타입입니다.
 */
class InputStreamSource(
    private val input: InputStream,
    private val timeout: Timeout = Timeout.NONE,
): Source {

    companion object: KLogging()

    /**
     * Okio I/O에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
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

    /**
     * Okio I/O에서 데이터를 읽어오는 `readAll` 함수를 제공합니다.
     */
    fun readAll(sink: Buffer): Long {
        var totalReadCount = 0L
        while (true) {
            val read = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (read <= 0L) break
            totalReadCount += read
        }
        return totalReadCount
    }

    /**
     * Okio I/O에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = timeout

    /**
     * Okio I/O 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        input.close()
    }
}
