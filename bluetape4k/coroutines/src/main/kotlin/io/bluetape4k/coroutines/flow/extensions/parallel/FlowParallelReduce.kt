package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireEquals
import kotlinx.coroutines.flow.FlowCollector

internal class FlowParallelReduce<T, R>(
    private val source: ParallelFlow<T>,
    private val seed: suspend () -> R,
    private val combine: suspend (R, T) -> R,
): ParallelFlow<R> {

    companion object: KLoggingChannel()

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism
        collectors.size.requireEquals(n, "collectors.size")
        val rails = Array(n) { ReducerCollector(combine) }

        repeat(n) {
            rails[it].accumulator = seed()
        }

        source.collect(*rails)

        repeat(n) {
            collectors[it].emit(rails[it].accumulator)
        }
    }

    private class ReducerCollector<T, R>(private val combine: suspend (R, T) -> R): FlowCollector<T> {

        @Suppress("UNCHECKED_CAST")
        var accumulator: R = null as R

        override suspend fun emit(value: T) {
            accumulator = combine(accumulator, value)
        }
    }
}
