package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSource
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Source
import okio.Timeout
import kotlin.coroutines.CoroutineContext

internal class ForwardingSuspendedSource(
    val delegate: Source,
    private val context: CoroutineContext = Dispatchers.IO,
): SuspendedSource {

    companion object: KLoggingChannel()

    override suspend fun read(sink: Buffer, byteCount: Long): Long = withContext(context) {
        delegate.read(sink, byteCount)
    }

    override suspend fun close() = withContext(context) {
        delegate.close()
    }

    override suspend fun timeout(): Timeout {
        return delegate.timeout()
    }
}
