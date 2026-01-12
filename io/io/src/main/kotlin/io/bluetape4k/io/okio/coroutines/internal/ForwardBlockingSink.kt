package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSink
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Sink
import okio.Timeout
import kotlin.coroutines.CoroutineContext

internal class ForwardBlockingSink(
    val delegate: SuspendedSink,
    private val context: CoroutineContext = Dispatchers.IO,
): Sink {

    companion object: KLoggingChannel()

    override fun write(source: Buffer, byteCount: Long) = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.write(source, byteCount)
        } ?: Unit
    }

    override fun flush() = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.flush()
        } ?: Unit
    }

    override fun close() = runBlocking(context) {
        withTimeoutOrNull(timeout()) {
            delegate.close()
        } ?: Unit
    }

    override fun timeout(): Timeout = delegate.timeout()
}
