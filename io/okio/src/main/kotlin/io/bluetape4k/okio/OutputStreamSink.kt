package io.bluetape4k.okio

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.io.OutputStream

/**
 * [OutputStream]을 Okio [Sink]로 변환합니다.
 *
 * ```kotlin
 * val baos = java.io.ByteArrayOutputStream()
 * val sink = baos.asSink()
 * val buffer = bufferOf("hello")
 * sink.write(buffer, buffer.size)
 * sink.flush()
 * val text = baos.toString(Charsets.UTF_8)
 * // text == "hello"
 * ```
 */
fun OutputStream.asSink(timeout: Timeout = Timeout.NONE) =
    OutputStreamSink(this, timeout)

/**
 * [OutputStream]을 Okio [Sink]로 감싼 구현체입니다.
 *
 * ```kotlin
 * val baos = java.io.ByteArrayOutputStream()
 * val sink = OutputStreamSink(baos)
 * val source = bufferOf("world")
 * sink.write(source, source.size)
 * sink.flush()
 * val text = baos.toString(Charsets.UTF_8)
 * // text == "world"
 * ```
 */
class OutputStreamSink(
    private val out: OutputStream,
    private val timeout: Timeout = Timeout.NONE,
): Sink {

    companion object: KLogging()

    /**
     * [source]에서 [byteCount] 바이트를 읽어 [OutputStream]에 씁니다.
     *
     * ```kotlin
     * val baos = java.io.ByteArrayOutputStream()
     * val sink = OutputStreamSink(baos)
     * val source = bufferOf("hello")
     * sink.write(source, 5L)
     * val text = baos.toString(Charsets.UTF_8)
     * // text == "hello"
     * ```
     *
     * @param source 읽을 데이터 버퍼
     * @param byteCount 쓸 바이트 수
     */
    override fun write(source: Buffer, byteCount: Long) {
        byteCount.requireInRange(0, source.size, "byteCount")

        Buffer.UnsafeCursor().use { cursor ->
            var remaining = byteCount
            while (remaining > 0) {
                timeout.throwIfReached()

                source.readUnsafe(cursor).use { ignored ->
                    cursor.seek(0)
                    val safeRemaining = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    val written = minOf(cursor.end - cursor.start, safeRemaining)
                    out.write(cursor.data, cursor.start, written)
                    log.debug { "OutputStream의 ${cursor.start} 위치에  $written bytes 를 썼습니다." }

                    remaining -= written.toLong()
                    source.skip(written.toLong())
                }
            }
        }
    }

    /**
     * Okio I/O 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override fun flush() {
        out.flush()
    }

    /**
     * Okio I/O에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = timeout

    /**
     * Okio I/O 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        out.close()
    }

    /**
     * Okio I/O 타입 변환을 위한 `toString` 함수를 제공합니다.
     */
    override fun toString(): String = "OutputStreamSink($out)"
}
