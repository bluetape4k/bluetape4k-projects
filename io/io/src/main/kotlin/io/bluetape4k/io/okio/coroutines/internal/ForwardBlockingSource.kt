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

internal class ForwardBlockingSource(
    val delegate: SuspendedSource,
    private val context: CoroutineContext = Dispatchers.IO,
): Source {

    companion object: KLoggingChannel()

    override fun read(sink: Buffer, byteCount: Long): Long = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.read(sink, byteCount)
        } ?: -1L
    }

    override fun close() = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.close()
        } ?: Unit
    }

    override fun timeout(): Timeout = delegate.timeout()
}
