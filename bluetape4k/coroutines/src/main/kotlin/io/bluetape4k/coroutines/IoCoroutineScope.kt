package io.bluetape4k.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

open class IoCoroutineScope: CoroutineScope {

    companion object: KLogging()

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
    }

    override fun toString(): String =
        "IoCoroutineScope(coroutineContext=$coroutineContext)"
}
