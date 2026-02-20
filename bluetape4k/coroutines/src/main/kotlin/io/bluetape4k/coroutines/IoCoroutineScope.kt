package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * [Dispatchers.IO] 기반의 기본 [CoroutineScope] 구현입니다.
 */
open class IoCoroutineScope: CloseableCoroutineScope() {

    companion object: KLoggingChannel()

    private val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    override fun close() {
        clearJobs()
    }

    override fun toString(): String =
        "IoCoroutineScope(coroutineContext=$coroutineContext)"
}
