package io.bluetape4k.resilience4j

import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * [coroutineContext] 가 취소 상태인지 확인합니다.
 *
 * @param coroutineContext 취소 상태인지 확인할 CoroutineContext
 * @param error           취소 상태인지 확인할 Throwable
 * @return 취소 상태인지 여부
 */
internal fun isCancellation(coroutineContext: CoroutineContext, error: Throwable? = null): Boolean {

    // job 이 없다면, `cancellation` 이 없다
    val job = coroutineContext[Job] ?: return false

    return job.isCancelled || (error != null && error is CancellationException)
}
