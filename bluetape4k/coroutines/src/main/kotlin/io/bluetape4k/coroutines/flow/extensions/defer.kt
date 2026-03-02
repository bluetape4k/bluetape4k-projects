package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * collect 시점에 Flow 공급 함수를 호출해 실제 Flow를 지연 생성합니다.
 *
 * ## 동작/계약
 * - collect마다 `flowSupplier()`를 새로 호출하므로 콜드(cold)하게 동작합니다.
 * - 공급 함수 또는 반환된 Flow에서 발생한 예외는 그대로 전파됩니다.
 * - 내부적으로 `emitAll`로 반환 Flow를 그대로 위임 수집합니다.
 *
 * ```kotlin
 * var n = 0
 * val f = defer { flowOf(++n) }
 * val result = listOf(f.first(), f.first())
 * // result == [1, 2]
 * ```
 *
 * @param flowSupplier collect 시 호출되어 방출 소스를 제공하는 함수입니다.
 */
inline fun <T> defer(
    crossinline flowSupplier: suspend () -> Flow<T>,
): Flow<T> = flow {
    emitAll(flowSupplier())
}
