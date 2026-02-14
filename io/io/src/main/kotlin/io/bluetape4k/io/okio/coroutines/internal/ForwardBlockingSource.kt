package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSource
import io.bluetape4k.io.okio.coroutines.withTimeoutOrNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Source
import okio.Timeout
import kotlin.coroutines.CoroutineContext

/**
 * Okio 코루틴에서 사용하는 `ForwardBlockingSource` 타입입니다.
 */
internal class ForwardBlockingSource(
    val delegate: SuspendedSource,
    private val context: CoroutineContext = Dispatchers.IO,
): Source {

    companion object: KLoggingChannel()

    /**
     * Okio 코루틴에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.read(sink, byteCount)
        } ?: -1L
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override fun close() = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.close()
        } ?: Unit
    }

    /**
     * Okio 코루틴에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = delegate.timeout()
}
