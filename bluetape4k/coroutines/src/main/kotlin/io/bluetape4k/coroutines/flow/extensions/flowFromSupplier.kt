package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * collect 시점에 supplier를 1회 호출해 단일 값을 방출하는 Flow를 만듭니다.
 *
 * ## 동작/계약
 * - collect마다 `function()`을 다시 호출하므로 콜드(cold) Flow로 동작합니다.
 * - supplier 예외는 그대로 collect 호출자에게 전파됩니다.
 * - 값 1개를 방출한 뒤 즉시 완료됩니다.
 *
 * ```kotlin
 * var n = 0
 * val f = flowFromSupplier { ++n }
 * val result = listOf(f.first(), f.first())
 * // result == [1, 2]
 * ```
 *
 * @param function 수집 시 호출할 값 공급 함수입니다.
 */
fun <T> flowFromSupplier(function: () -> T): Flow<T> = FlowFromSupplier(function)

private class FlowFromSupplier<T>(private val supplier: () -> T): Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        val value = supplier()
        collector.emit(value)
    }
}
