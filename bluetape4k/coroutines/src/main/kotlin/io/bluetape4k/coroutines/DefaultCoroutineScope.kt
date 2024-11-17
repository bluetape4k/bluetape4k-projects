package io.bluetape4k.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

/**
 * 기본 [CoroutineScope] 입니다
 */
open class DefaultCoroutineScope: CoroutineScope {

    private val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    /**
     * 자식의 모든 Job을 취소합니다.
     *
     * @param cause 취소 사유에 해당하는 예외정보. default is null
     */
    fun clearJobs(cause: CancellationException? = null) {
        coroutineContext.cancelChildren(cause)
    }
}
