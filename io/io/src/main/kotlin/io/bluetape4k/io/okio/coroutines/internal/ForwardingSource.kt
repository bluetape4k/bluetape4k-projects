package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendSource
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Source
import okio.Timeout

internal class ForwardingSource(
    val delegate: SuspendSource,
): Source {

    companion object: KLoggingChannel()

    private val timeout = Timeout()

    override fun read(sink: Buffer, byteCount: Long): Long = runBlocking {
        withTimeout(timeout) {
            delegate.read(sink, byteCount)
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
