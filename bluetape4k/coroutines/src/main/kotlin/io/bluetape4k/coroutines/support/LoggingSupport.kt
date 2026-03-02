package io.bluetape4k.coroutines.support

import io.bluetape4k.LibraryName
import io.bluetape4k.coroutines.context.PropertyCoroutineContext
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal val log by lazy { KotlinLogging.logger(LibraryName) }

/**
 * `Job` 완료 시점을 태그와 함께 디버그 로그로 남기고 원본 `Job`을 그대로 반환합니다.
 *
 * ## 동작/계약
 * - `invokeOnCompletion(onCancelling = true)` 핸들러를 등록하므로 취소 시점에도 로그가 남습니다.
 * - 완료 원인이 `CancellationException`이면 `[$tag] 🔥`, 그 외(정상 완료/실패)는 `[$tag] ✅` 형식으로 기록합니다.
 * - 수신 `Job` 자체를 새로 만들지 않으며 `apply`로 같은 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val job = launch { /* do work */ }.log("sync")
 * // job == 원본 Job 인스턴스
 * // 로그 예: [sync] ✅ 또는 [sync] 🔥
 * ```
 * @param tag 로그 메시지 앞에 출력할 태그 값입니다.
 */
fun <T: Job> T.log(tag: Any): T = apply {
    invokeOnCompletion(onCancelling = true, invokeImmediately = false) {
        if (it is CancellationException) {
            log.debug { "[$tag] 🔥" }
        } else {
            log.debug(it) { "[$tag] ✅" }
        }
    }
}

/**
 * 현재 코루틴 컨텍스트 정보를 포함해 메시지를 디버그 로그로 기록합니다.
 *
 * ## 동작/계약
 * - `msg()`를 한 번 호출해 메시지를 만든 뒤 로그를 출력합니다.
 * - `CoroutineName`과 `PropertyCoroutineContext`가 있으면 `[name, props] message` 형식, 없으면 가능한 정보만 출력합니다.
 * - 이 함수는 값을 반환하지 않고 로그 출력만 수행합니다.
 *
 * ```kotlin
 * suspendLogging { "loading" }
 * // result == 로그 출력 완료(Unit)
 * // 로그 예: [worker-1, {traceId=abc}] loading
 * ```
 * @param msg 로그로 출력할 메시지를 생성하는 suspend 함수입니다.
 */
suspend inline fun suspendLogging(crossinline msg: suspend () -> Any?) = coroutineScope {
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
 * 문자열 메시지를 받아 [suspendLogging]으로 기록합니다.
 *
 * ## 동작/계약
 * - 전달된 `msg`를 그대로 캡처하여 `suspendLogging { msg }`로 위임합니다.
 * - 코루틴 이름/속성 출력 규칙은 [suspendLogging]과 동일합니다.
 * - 이 함수는 값을 반환하지 않고 로그 출력만 수행합니다.
 *
 * ```kotlin
 * suspendLogging("connected")
 * // result == 로그 출력 완료(Unit)
 * // 로그 예: [api] connected
 * ```
 * @param msg 로그로 출력할 문자열입니다.
 */
suspend fun suspendLogging(msg: String) {
    suspendLogging { msg }
}
