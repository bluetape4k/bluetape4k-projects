package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * collect 시점에 suspend 공급 함수를 1회 호출해 값을 방출하는 Flow를 만듭니다.
 *
 * ## 동작/계약
 * - collect마다 `function()`을 다시 호출하므로 콜드(cold)하게 동작합니다.
 * - 공급 함수에서 발생한 예외는 그대로 하류로 전파됩니다.
 * - 값 1개를 방출한 뒤 완료됩니다.
 *
 * ```kotlin
 * var n = 0
 * val f = flowFromSuspend { ++n }
 * val result = listOf(f.first(), f.first())
 * // result == [1, 2]
 * ```
 *
 * @param function collect 시 실행할 suspend 공급 함수입니다.
 */
fun <T> flowFromSuspend(function: suspend () -> T): Flow<T> = FlowFromSuspend(function)

private class FlowFromSuspend<T>(private val supplier: suspend () -> T): Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.emit(supplier())
    }
}
