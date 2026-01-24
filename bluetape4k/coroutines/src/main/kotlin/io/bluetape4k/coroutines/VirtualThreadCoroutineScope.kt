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

open class VirtualThreadCoroutineScope: CoroutineScope, Closeable {
    companion object: KLogging() {
        val Instance: VirtualThreadCoroutineScope by lazy { VirtualThreadCoroutineScope() }
    }

    private val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.VT + job

    fun clearJobs(cause: CancellationException? = null) {
        log.debug { "clearJobs: cause=$cause" }
        coroutineContext.cancelChildren(cause)
        coroutineContext.cancel()
    }

    override fun close() {
        clearJobs()
    }

    override fun toString(): String =
        "VirtualThreadCoroutineScope(coroutineContext=$coroutineContext)"
}
