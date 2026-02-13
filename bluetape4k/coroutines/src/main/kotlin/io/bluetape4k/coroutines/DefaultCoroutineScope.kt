package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import java.io.Closeable
import kotlin.coroutines.CoroutineContext

/**
 * 기본 [CoroutineScope] 입니다
 */
open class DefaultCoroutineScope: CoroutineScope, Closeable {

    companion object: KLoggingChannel()

    private val job: CompletableJob = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    /**
     * 자식 Job들을 취소하고 현재 scope도 취소합니다.
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
        "DefaultCoroutineScope(coroutineContext=$coroutineContext)"
}
