package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

abstract class CloseableCoroutineScope: CoroutineScope, Closeable {

    companion object: KLoggingChannel()

    protected val closed = AtomicBoolean(false)
    protected val cancelled = AtomicBoolean(false)

    val scopeClosed: Boolean get() = closed.get()
    val scopeCancelled: Boolean get() = cancelled.get()

    /**
     * 자식 Job들을 취소하고 현재 scope도 취소합니다.
     *
     * @param cause 취소 사유에 해당하는 예외정보. default is null
     */
    fun clearJobs(cause: CancellationException? = null) {
        if (cancelled.compareAndSet(false, true)) {
            log.debug { "clearJobs: cause=$cause" }
            coroutineContext.cancelChildren(cause)
            coroutineContext.cancel(cause)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            clearJobs()
        }
    }
}
