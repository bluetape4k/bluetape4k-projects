package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSource
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.Source
import okio.Timeout

/**
 * Okio 코루틴에서 사용하는 `ForwardSuspendedSource` 타입입니다.
 */
internal class ForwardSuspendedSource(val delegate: Source): SuspendedSource {

    companion object: KLoggingChannel()

    /**
     * Okio 코루틴에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override suspend fun read(sink: Buffer, byteCount: Long): Long {
        return delegate.read(sink, byteCount)
    }

    /**
     * Okio 코루틴 리소스를 정리하고 닫습니다.
     */
    override suspend fun close() {
        delegate.close()
    }

    /**
     * Okio 코루틴에서 `timeout` 함수를 제공합니다.
     */
    override fun timeout(): Timeout = delegate.timeout()
}
