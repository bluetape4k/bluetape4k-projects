package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireEquals
import kotlinx.coroutines.flow.FlowCollector

internal class FlowParallelMap<T, R>(
    private val source: ParallelFlow<T>,
    private val mapper: suspend (T) -> R,
): ParallelFlow<R> {

    companion object: KLoggingChannel()

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism
        collectors.size.requireEquals(n, "collectors.size")
        val rails = Array(n) { MapperCollector(collectors[it], mapper) }

        source.collect(*rails)
    }

    private class MapperCollector<T, R>(
        val collector: FlowCollector<R>,
        val mapper: suspend (T) -> R,
    ): FlowCollector<T> {
        override suspend fun emit(value: T) {
            collector.emit(mapper(value))
        }
    }
}
