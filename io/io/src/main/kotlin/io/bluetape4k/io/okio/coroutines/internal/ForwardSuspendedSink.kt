package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.io.okio.coroutines.SuspendedSink
import io.bluetape4k.logging.coroutines.KLoggingChannel
import okio.Buffer
import okio.Sink
import okio.Timeout

/**
 * Okio 코루틴에서 사용하는 `ForwardSuspendedSink` 타입입니다.
 */
internal class ForwardSuspendedSink(val delegate: Sink): SuspendedSink {

    companion object: KLoggingChannel()

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override suspend fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
    }

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override suspend fun flush() {
        delegate.flush()
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
