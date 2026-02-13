package io.bluetape4k.coroutines

import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.KLogging
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

/**
 * Virtual Thread dispatcher 기반 [CoroutineScope] 구현입니다.
 */
open class VirtualThreadCoroutineScope: CoroutineScope, Closeable {
    companion object: KLogging() {
        val Instance: VirtualThreadCoroutineScope by lazy { VirtualThreadCoroutineScope() }
    }

    private val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.VT + job

    /**
     * 자식 Job들을 취소하고 현재 scope도 취소합니다.
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
        "VirtualThreadCoroutineScope(coroutineContext=$coroutineContext)"
}
