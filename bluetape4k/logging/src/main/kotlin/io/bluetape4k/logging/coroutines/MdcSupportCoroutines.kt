package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.withLoggingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

/**
 * 코루틴 컨텍스트에서 단일 MDC 키-값을 적용한 뒤 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `mapOf(pair)` 오버로드에 위임합니다.
 * - `restorePrevious`는 MDC 복원 전략에 동일하게 전달됩니다.
 *
 * @param pair MDC에 적용할 키-값입니다.
 * @param restorePrevious 기존 MDC 값을 복원할지 여부입니다.
 * @param block MDC가 적용된 코루틴 블록입니다.
 */
suspend inline fun <T> withCoroutineLoggingContext(
    pair: Pair<String, Any?>,
    restorePrevious: Boolean = true,
    crossinline block: suspend CoroutineScope.() -> T,
): T =
    withCoroutineLoggingContext(mapOf(pair), restorePrevious) { block(this) }

/**
 * 코루틴 컨텍스트에서 여러 MDC 키-값을 적용한 뒤 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `pairs.toMap()`으로 변환해 map 오버로드에 위임합니다.
 * - 중복 키가 있으면 마지막 값이 적용됩니다.
 *
 * @param pairs MDC에 적용할 키-값 목록입니다.
 * @param restorePrevious 기존 MDC 값을 복원할지 여부입니다.
 * @param block MDC가 적용된 코루틴 블록입니다.
 */
suspend inline fun <T> withCoroutineLoggingContext(
    vararg pairs: Pair<String, Any?>,
    restorePrevious: Boolean = true,
    crossinline block: suspend CoroutineScope.() -> T,
): T =
    withCoroutineLoggingContext(pairs.toMap(), restorePrevious, block)

/**
 * 코루틴 컨텍스트에서 MDC를 적용한 뒤 블록을 실행합니다.
 *
 * ## 동작/계약
 * - `withContext(MDCContext())` 안에서 실행되어 코루틴 전환 시 MDC가 전파됩니다.
 * - `restorePrevious=true`면 `withLoggingContext(map, true)`로 복원 정책을 따릅니다.
 * - `restorePrevious=false`면 블록 종료 후 적용 키를 `MDC.remove`로 정리합니다.
 *
 * ```kotlin
 * withCoroutineLoggingContext(mapOf("traceId" to "t-1")) {
 *   // MDCContext와 함께 실행
 * }
 * ```
 *
 * @param map MDC에 적용할 키-값 맵입니다.
 * @param restorePrevious 기존 MDC 값을 복원할지 여부입니다.
 * @param block MDC가 적용된 코루틴 블록입니다.
 */
suspend inline fun <T> withCoroutineLoggingContext(
    map: Map<String, Any?>,
    restorePrevious: Boolean = true,
    crossinline block: suspend CoroutineScope.() -> T,
): T {
    return if (restorePrevious) {
        withContext(MDCContext()) {
            withLoggingContext(map, restorePrevious) {
                block(this)
            }
        }
    } else {
        val keysToRemove = map.filterValues { it != null }.keys
        try {
            withContext(MDCContext()) {
                withLoggingContext(map, false) {
                    block(this)
                }
            }
        } finally {
            keysToRemove.forEach { MDC.remove(it) }
        }
    }
}
