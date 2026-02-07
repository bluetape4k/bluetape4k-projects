package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireInRange
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.io.OutputStream

fun OutputStream.asSink(timeout: Timeout = Timeout.NONE) =
    OutputStreamSink(this, timeout)

class OutputStreamSink(
    private val out: OutputStream,
    private val timeout: Timeout = Timeout.NONE,
): Sink {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        byteCount.requireInRange(0, source.size, "byteCount")

        Buffer.UnsafeCursor().use { cursor ->
            var remaining = byteCount
            while (remaining > 0) {
                timeout.throwIfReached()

                source.readUnsafe(cursor).use { ignored ->
                    cursor.seek(0)
                    val written = minOf(cursor.end - cursor.start, remaining.toInt())
                    out.write(cursor.data, cursor.start, written)
                    log.debug { "OutputStream의 ${cursor.start} 위치에  $written bytes 를 썼습니다." }

                    remaining -= written.toLong()
                    source.skip(written.toLong())
                }
            }
        }
    }

    override fun flush() {
        out.flush()
    }

    override fun timeout(): Timeout = timeout

    override fun close() {
        out.close()
    }

    override fun toString(): String = "OutputStreamSink($out)"
}
