package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * 주어진 suspend [function]의 반환 값을 하나의 항목으로 발행하는 _cold_ flow
 *
 * Example of usage:
 *
 * ```
 * suspend fun remoteCall(): R = ...
 * fun remoteCallFlow(): Flow<R> = flowFromSuspend(::remoteCall)
 * ```
 * @param function the function that produces a single value
 */
fun <T> flowFromSuspend(function: suspend () -> T): Flow<T> = FlowFromSuspend(function)

private class FlowFromSuspend<T>(private val supplier: suspend () -> T): Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.emit(supplier())
    }
}
