package io.bluetape4k.coroutines.support

import io.bluetape4k.coroutines.context.PropertyCoroutineContext
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

@PublishedApi
internal val log = KotlinLogging.logger { }

/**
 * Job이 완료되거나 취소되면 로그를 남깁니다.
 *
 * ```
 * val job = launch { ... }.log("tag")
 * job.cancel()  // [tag] Cancelled
 * ```
 *
 * @param tag 로그에 표시할 태그
 */
fun <T: Job> T.log(tag: Any): T = apply {
    invokeOnCompletion(onCancelling = true, invokeImmediately = false) {
        if (it is CancellationException) {
            log.debug { "[$tag] Cancelled" }
        } else {
            log.debug(it) { "[$tag] Completed" }
        }
    }
}

/**
 * Coroutine Context 에서 CoroutineName 과 PropertyCoroutineContext 를 참조하여 로그를 남깁니다.
 *
 * ```
 * val task = async { ... }
 * coLogging { task.await() }
 * ```
 *
 * @param msg 로그에 표시할 메시지
 */
suspend fun coLogging(msg: suspend () -> Any?) {
    val name = coroutineContext[CoroutineName]?.name
    val props = coroutineContext[PropertyCoroutineContext]?.properties

    val msgText = msg.invoke()
    if (props != null) {
        if (name != null) {
            log.debug { "[$name, $props] $msgText" }
        } else {
            log.debug { "[$props] $msgText" }
        }
    } else if (name != null) {
        log.debug { "[$name] $msgText" }
    } else {
        log.debug { msgText }
    }
}

/**
 * Coroutine Context 에서 CoroutineName 과 PropertyCoroutineContext 를 참조하여 로그를 남깁니다.
 *
 * ```
 * coLogging { "message" }
 * ```
 *
 * @param msg 로그에 표시할 메시지
 */
suspend fun coLogging(msg: String) {
    coLogging { msg }
}
