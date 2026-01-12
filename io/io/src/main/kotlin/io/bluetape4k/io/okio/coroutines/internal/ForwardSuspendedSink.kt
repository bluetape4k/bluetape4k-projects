package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSink
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.Sink
import okio.Timeout

internal class ForwardSuspendedSink(val delegate: Sink): SuspendedSink {

    companion object: KLoggingChannel()

    override suspend fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
    }

    override suspend fun flush() {
        delegate.flush()
    }

    override suspend fun close() {
        delegate.close()
    }

    override fun timeout(): Timeout = delegate.timeout()
}
