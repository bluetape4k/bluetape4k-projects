package io.bluetape4k.logging

import org.slf4j.MDC
import java.util.Collections
import java.util.LinkedHashMap

/**
 * 단일 키-값을 MDC에 적용한 범위 안에서 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 값이 `null`이면 MDC를 건드리지 않고 블록만 실행합니다.
 * - `restorePrevious=true`면 기존 값을 복원하고, `false`면 스코프 종료 시 키를 제거합니다.
 * - 내부적으로 `MDC.putCloseable(...).use { ... }`를 사용해 범위 적용합니다.
 *
 * ```kotlin
 * withLoggingContext("traceId" to "t-1") {
 *   // MDC["traceId"] == "t-1"
 * }
 * ```
 *
 * @param pair MDC에 적용할 키-값입니다.
 * @param restorePrevious 이전 값을 복원할지 여부입니다.
 * @param block MDC가 적용된 상태에서 실행할 코드입니다.
 */
inline fun <T> withLoggingContext(
    pair: Pair<String, Any?>,
    restorePrevious: Boolean = true,
    block: () -> T,
): T = when {
    pair.second == null -> block()
    !restorePrevious -> MDC.putCloseable(pair.first, pair.second.toString()).use { block() }
    else             -> {
        val prevValue = MDC.get(pair.first)
        try {
            MDC.putCloseable(pair.first, pair.second.toString()).use { block() }
        } finally {
            prevValue?.let { MDC.put(pair.first, it) }
        }
    }
}

/**
 * 여러 키-값을 MDC에 적용한 범위 안에서 블록을 실행합니다.
 *
 * ## 동작/계약
 * - `null` 값은 자동으로 제외됩니다.
 * - 실제 적용은 `Map` 오버로드에 위임됩니다.
 *
 * ```kotlin
 * withLoggingContext("traceId" to "t-1", "userId" to 10L) {
 *   // 두 키가 MDC에 적용됨
 * }
 * ```
 *
 * @param pairs MDC에 적용할 키-값 목록입니다.
 * @param restorePrevious 이전 값을 복원할지 여부입니다.
 * @param block MDC가 적용된 상태에서 실행할 코드입니다.
 */
inline fun <T> withLoggingContext(
    vararg pairs: Pair<String, Any?>,
    restorePrevious: Boolean = true,
    block: () -> T,
): T =
    withLoggingContext(pairs.filter { it.second != null }.toMap(), restorePrevious, block)

/**
 * 맵 형태 MDC 값을 적용한 범위 안에서 블록을 실행합니다.
 *
 * ## 동작/계약
 * - `null` 값 항목은 적용하지 않습니다.
 * - 블록 종료 시 각 키를 이전 값으로 복원하거나 제거합니다.
 * - 복원 콜백에서 발생한 예외는 `runCatching`으로 무시합니다.
 *
 * ```kotlin
 * withLoggingContext(mapOf("traceId" to "t-1")) {
 *   // MDC["traceId"] == "t-1"
 * }
 * ```
 *
 * @param map MDC에 적용할 키-값 맵입니다.
 * @param restorePrevious 이전 값을 복원할지 여부입니다.
 * @param block MDC가 적용된 상태에서 실행할 코드입니다.
 */
inline fun <T> withLoggingContext(
    map: Map<String, Any?>,
    restorePrevious: Boolean = true,
    block: () -> T,
): T {
    val mdcMap = map.filter { it.value != null }
    if (mdcMap.isEmpty()) {
        return block()
    }
    val cleanupCallbacks: List<() -> Unit> = mdcMap.keys.map { key ->
        val prevValue = MDC.get(key)
        if (prevValue != null && restorePrevious) {
            { MDC.put(key, prevValue) }
        } else {
            { MDC.remove(key) }
        }
    }

    return try {
        mdcMap.forEach { (key, value) ->
            MDC.put(key, value.toString())
        }
        block()
    } finally {
        cleanupCallbacks.forEach { callback ->
            runCatching { callback() }
        }
    }
}

private fun immutableMdcCopy(context: Map<String, String>): Map<String, String> =
    Collections.unmodifiableMap(LinkedHashMap(context))

/**
 * 현재 thread의 MDC 전체를 caller와 분리된 읽기 전용 MDC map으로 복사합니다.
 *
 * MDC에는 정제된 비밀이 아닌 식별자만 넣으세요. 이 함수는 raw token, header, payload를
 * 검증하거나 가리지 않고 그대로 복사합니다. 비용은 MDC 항목 수에 비례하므로
 * context는 작은 low-cardinality 식별자 집합으로 유지하세요.
 */
fun captureMdcContext(): Map<String, String> =
    MDC.getCopyOfContextMap()?.let(::immutableMdcCopy) ?: emptyMap()

private fun replaceMdcContext(context: Map<String, String>) {
    if (context.isEmpty()) {
        MDC.clear()
    } else {
        MDC.setContextMap(context)
    }
}

/** 전체 MDC를 scope 동안 대체하고 정상·예외 모두 scope 진입 전 context를 복원합니다. */
fun <T> withMdcContext(context: Map<String, String>, block: () -> T): T {
    val previous = captureMdcContext()
    val applied = immutableMdcCopy(context)
    return try {
        replaceMdcContext(applied)
        block()
    } finally {
        replaceMdcContext(previous)
    }
}
