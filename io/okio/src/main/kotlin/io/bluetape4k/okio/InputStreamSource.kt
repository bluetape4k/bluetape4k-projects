package io.bluetape4k.okio

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.Source
import okio.Timeout
import java.io.InputStream

/**
 * [InputStream]을 Okio [Source]로 변환합니다.
 *
 * ```kotlin
 * val bytes = "hello".toByteArray()
 * val source = bytes.inputStream().asSource()
 * val buffer = Buffer()
 * source.read(buffer, 5L)
 * val text = buffer.readUtf8()
 * // text == "hello"
 * ```
 */
fun InputStream.asSource(timeout: Timeout = Timeout.NONE) =
    InputStreamSource(this, timeout)

/**
 * [InputStream]을 Okio [Source]로 감싼 구현체입니다.
 *
 * ```kotlin
 * val bytes = "hello world".toByteArray()
 * val source = InputStreamSource(bytes.inputStream())
 * val sink = Buffer()
 * val read = source.readAll(sink)
 * // read == 11L
 * source.close()
 * ```
 */
class InputStreamSource(
    private val input: InputStream,
    private val timeout: Timeout = Timeout.NONE,
): Source {

    companion object: KLogging()

    /**
     * [InputStream]에서 최대 [byteCount] 바이트를 읽어 [sink]에 씁니다.
     *
     * ```kotlin
     * val source = InputStreamSource("hi".toByteArray().inputStream())
     * val sink = Buffer()
     * val read = source.read(sink, 2L)
     * // read == 2L
     * ```
     *
     * @param sink 데이터를 쓸 버퍼
     * @param byteCount 읽을 최대 바이트 수
     * @return 실제 읽은 바이트 수, EOF이면 -1
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
     * [InputStream]의 모든 내용을 읽어 [sink]에 씁니다.
     *
     * ```kotlin
     * val source = InputStreamSource("hello".toByteArray().inputStream())
     * val sink = Buffer()
     * val total = source.readAll(sink)
     * // total == 5L
     * ```
     *
     * @param sink 데이터를 쓸 버퍼
     * @return 읽은 총 바이트 수
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
