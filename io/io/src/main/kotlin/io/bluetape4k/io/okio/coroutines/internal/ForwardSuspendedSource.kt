package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSource
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.Source
import okio.Timeout

internal class ForwardSuspendedSource(val delegate: Source): SuspendedSource {

    companion object: KLoggingChannel()

    override suspend fun read(sink: Buffer, byteCount: Long): Long {
        return delegate.read(sink, byteCount)
    }

    override suspend fun close() {
        delegate.close()
    }

    override fun timeout(): Timeout = delegate.timeout()
}
