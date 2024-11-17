package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.AsyncSource
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Source
import okio.Timeout

internal class ForwardingSource(
    val delegate: AsyncSource,
): Source {

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
