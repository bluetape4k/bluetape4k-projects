package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSink
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Sink
import okio.Timeout
import kotlin.coroutines.CoroutineContext

internal class ForwardingSuspendSink(
    val delegate: Sink,
    private val context: CoroutineContext = Dispatchers.IO,
): SuspendedSink {

    companion object: KLoggingChannel()

    override suspend fun write(source: Buffer, byteCount: Long) {
        withContext(context) {
            delegate.write(source, byteCount)
        }
    }

    override suspend fun flush() {
        withContext(context) {
            delegate.flush()
        }
    }

    override suspend fun close() {
        withContext(context) {
            delegate.close()
        }
    }

    override suspend fun timeout(): Timeout {
        return delegate.timeout()
    }
}
