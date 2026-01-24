package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [source]의 값들을 병렬로 reduce하고 그 값을 emit합니다.
 */
internal class FlowParallelReduceSequential<T>(
    private val source: ParallelFlow<T>,
    private val combine: suspend (T, T) -> T,
): AbstractFlow<T>() {

    companion object: KLoggingChannel()

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val n = source.parallelism
        val rails = Array(n) { ReducerCollector(combine) }

        source.collect(*rails)

        var accumulator: T = uninitialized()
        var hasValue = false

        rails.forEach { rail ->
            if (!hasValue && rail.hasValue.get()) {
                accumulator = rail.accumulator
                hasValue = true
            } else if (hasValue && rail.hasValue.get()) {
                accumulator = combine(accumulator, rail.accumulator)
            }
        }

        if (hasValue) {
            collector.emit(accumulator)
        }
    }

    class ReducerCollector<T>(private val combine: suspend (T, T) -> T): FlowCollector<T> {
        var accumulator: T = uninitialized()
        val hasValue = AtomicBoolean(false)

        override suspend fun emit(value: T) {
            if (hasValue.get()) {
                accumulator = combine(accumulator, value)
            } else {
                hasValue.set(true)
                accumulator = value
            }
        }
    }
}
