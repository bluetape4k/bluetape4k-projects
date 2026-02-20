package io.bluetape4k.coroutines

import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Virtual Thread dispatcher 기반 [CoroutineScope] 구현입니다.
 */
open class VirtualThreadCoroutineScope: CloseableCoroutineScope() {

    companion object: KLoggingChannel()

    private val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.VT + job

    override fun toString(): String =
        "VirtualThreadCoroutineScope(coroutineContext=$coroutineContext)"
}
