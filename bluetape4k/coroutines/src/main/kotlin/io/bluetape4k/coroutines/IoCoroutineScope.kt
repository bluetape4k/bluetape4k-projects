package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

open class IoCoroutineScope: CoroutineScope, Closeable {

    companion object: KLoggingChannel()

    private val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    /**
     * 자식의 모든 Job을 취소합니다.
     *
     * @param cause 취소 사유에 해당하는 예외정보. default is null
     */
    fun clearJobs(cause: CancellationException? = null) {
        log.debug { "clearJobs: cause=$cause" }
        coroutineContext.cancelChildren(cause)
        coroutineContext.cancel(cause)
    }

    override fun close() {
        clearJobs()
    }

    override fun toString(): String =
        "IoCoroutineScope(coroutineContext=$coroutineContext)"
}
