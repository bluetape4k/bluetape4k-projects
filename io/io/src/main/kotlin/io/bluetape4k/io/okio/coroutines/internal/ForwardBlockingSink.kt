package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSink
import io.bluetape4k.io.okio.coroutines.withTimeoutOrNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.io.InterruptedIOException
import kotlin.coroutines.CoroutineContext

/**
 * Okio 코루틴에서 사용하는 `ForwardBlockingSink` 타입입니다.
 */
internal class ForwardBlockingSink(
    val delegate: SuspendedSink,
    private val context: CoroutineContext = Dispatchers.IO,
): Sink {

    companion object: KLoggingChannel()

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(source: Buffer, byteCount: Long) = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.write(source, byteCount)
        } ?: throw InterruptedIOException("Timed out while writing to suspended sink.")
    }

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override fun flush() = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.flush()
        } ?: throw InterruptedIOException("Timed out while flushing suspended sink.")
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override fun close() = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.close()
        } ?: throw InterruptedIOException("Timed out while closing suspended sink.")
    }

    /**
     * Okio 코루틴에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = delegate.timeout()
}
