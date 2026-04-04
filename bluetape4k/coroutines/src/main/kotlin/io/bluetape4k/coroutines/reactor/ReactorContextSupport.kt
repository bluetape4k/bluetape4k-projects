package io.bluetape4k.coroutines.reactor

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import reactor.util.context.Context
import kotlin.coroutines.CoroutineContext

/**
 * 현재 코루틴 컨텍스트에서 Reactor [Context]를 조회합니다.
 *
 * ## 동작/계약
 * - `currentCoroutineContext()[ReactorContext]?.context`를 그대로 반환합니다.
 * - Reactor 컨텍스트가 없으면 `null`을 반환합니다.
 * - 조회 전용이며 코루틴/리액터 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val ctx = currentReactiveContext()
 * // ctx == null 또는 Reactor Context
 * ```
 */
suspend inline fun currentReactiveContext(): Context? =
    currentCoroutineContext()[ReactorContext]?.context

/**
 * 지정한 [CoroutineContext]에서 Reactor [Context]를 조회합니다.
 *
 * ## 동작/계약
 * - `ReactorContext` 요소가 있으면 내부 `context`를 반환합니다.
 * - 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val ctx = coroutineContext.getReactiveContext()
 * // ctx == null 또는 Reactor Context
 * ```
 *
 * @receiver 조회 대상 코루틴 컨텍스트입니다.
 */
fun CoroutineContext.getReactiveContext(): Context? =
    this[ReactorContext]?.context

/**
 * Reactor [Context]에서 키가 있을 때만 값을 꺼냅니다.
 *
 * ## 동작/계약
 * - `hasKey(key)`가 `true`일 때 `get(key)`를 호출합니다.
 * - 키가 없으면 예외 없이 `null`을 반환합니다.
 * - 타입이 맞지 않으면 `ClassCastException`이 발생할 수 있습니다.
 *
 * ```kotlin
 * val value: String? = context.getOrNull("traceId")
 * // key가 없으면 null
 * ```
 *
 * @param key 조회할 키입니다.
 */
fun <T: Any> Context.getOrNull(key: Any): T? {
    return if (hasKey(key)) get(key) else null
}

/**
 * 현재 코루틴의 Reactor 컨텍스트에서 키 값을 조회합니다.
 *
 * ## 동작/계약
 * - Reactor 컨텍스트가 없거나 키가 없으면 `null`을 반환합니다.
 * - 값 타입이 기대 타입과 다르면 `ClassCastException`이 발생할 수 있습니다.
 *
 * ```kotlin
 * val traceId: String? = getReactorContextValueOrNull("traceId")
 * // traceId == null (Reactor 컨텍스트 없을 때)
 * ```
 *
 * @param key 조회할 키입니다.
 */
suspend inline fun <T: Any> getReactorContextValueOrNull(key: Any): T? =
    currentReactiveContext()?.getOrNull(key)

/**
 * 주어진 코루틴 컨텍스트의 Reactor 컨텍스트에서 키 값을 조회합니다.
 *
 * ## 동작/계약
 * - Reactor 컨텍스트가 없거나 키가 없으면 `null`을 반환합니다.
 * - 값 타입이 기대 타입과 다르면 `ClassCastException`이 발생할 수 있습니다.
 *
 * ```kotlin
 * val traceId: String? = coroutineContext.getReactorContextValueOrNull("traceId")
 * // traceId == null (Reactor 컨텍스트 없을 때)
 * ```
 *
 * @param key 조회할 키입니다.
 */
fun <T: Any> CoroutineContext.getReactorContextValueOrNull(key: Any): T? {
    return getReactiveContext()?.getOrNull(key)
}
