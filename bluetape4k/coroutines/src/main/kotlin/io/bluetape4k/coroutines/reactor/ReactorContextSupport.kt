package io.bluetape4k.coroutines.reactor

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import reactor.util.context.Context
import kotlin.coroutines.CoroutineContext

/**
 * 현 Coroutine Context에서 Reactor용 [Context] 를 가져옵니다. 없다면 null 반환
 *
 * ```
 * private val key = "answer"
 * private val value = "42"
 * var captured: String? = null
 *
 * val flow = flow {
 *     // captured = currentCoroutineContext()[ReactorContext]?.context?.getOrNull(key)
 *     captured = currentReactiveContext()?.getOrNull(key)
 *     emit("A")
 * }
 *
 * flow.asFlux()
 *     .contextWrite { ctx -> ctx.put(key, value) }
 *     .subscribe()
 *
 * captured shouldBeEqualTo value
 * ```
 */
suspend inline fun currentReactiveContext(): Context? =
    currentCoroutineContext()[ReactorContext]?.context

/**
 * [CoroutineContext]에서 Reactor용 [Context]를 조회합니다.
 */
fun CoroutineContext.getReactiveContext(): Context? =
    this[ReactorContext]?.context

/**
 * Reactor용 [Context]에 [key]에 해당하는 값을 가져옵니다. 없다면 null 반환
 *
 * ```
 * private val key = "answer"
 * private val value = "42"
 *
 * var captured: String? = null
 * val flux = Flux.just("A")
 *     .contextWrite { context ->
 *         captured = context.getOrNull(key)
 *         context
 *     }
 * flux.awaitFirst()
 * captured.shouldBeNull()
 * ```
 */
fun <T: Any> Context.getOrNull(key: Any): T? {
    return if (hasKey(key)) get(key) else null
}

/**
 * 현 Coroutine Context에서 Reactor용 [Context]의 [key]에 해당하는 값을 가져옵니다. 없다면 null 반환
 *
 * ```
 * private val key = "answer"
 * private val value = "42"
 *
 * var captured: String? = null
 * val flow = flow {
 *     captured = getReactorContextValueOrNull(key)
 *     emit("A")
 * }
 * // ReactorContext에 아무 값도 전달되지 않았으므로, captured는 null입니다.
 * flow.asFlux()
 *     .subscribe()
 * captured.shouldBeNull()
 * ```
 */
suspend inline fun <T: Any> getReactorContextValueOrNull(key: Any): T? =
    currentReactiveContext()?.getOrNull(key)

/**
 * Reactor [Context]에 저장된 정보를 Coroutines 환경 하에서 사용하기 위한 확장 함수입니다.
 *
 * ```
 * private val key = "answer"
 * private val value = "42"
 *
 * var captured: String? = null
 * val flow = flow {
 *     captured = getReactorContextValueOrNull(key)
 *     emit("A")
 * }
 * // ReactorContext에 아무 값도 전달되지 않았으므로, captured는 null입니다.
 * flow.asFlux()
 *     .subscribe()
 * captured.shouldBeNull()
 * ```
 */
fun <T: Any> CoroutineContext.getReactorContextValueOrNull(key: Any): T? {
    return getReactiveContext()?.getOrNull(key)
}
