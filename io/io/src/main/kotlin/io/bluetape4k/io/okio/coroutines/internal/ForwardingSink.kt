package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.AsyncSink
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Sink
import okio.Timeout

internal class ForwardingSink(
    val delegate: AsyncSink,
): Sink {

    companion object: KLoggingChannel()

    val timeout = Timeout()

    override fun write(source: Buffer, byteCount: Long) = runBlocking {
        withTimeout(timeout) {
            delegate.write(source, byteCount)
        }
    }

    override fun flush() = runBlocking {
        withTimeout(timeout) {
            delegate.flush()
        }
    }

    override fun close() = runBlocking {
        withTimeout(timeout) {
            delegate.close()
        }
    }

    override fun timeout(): Timeout {
        return timeout
    }
}
