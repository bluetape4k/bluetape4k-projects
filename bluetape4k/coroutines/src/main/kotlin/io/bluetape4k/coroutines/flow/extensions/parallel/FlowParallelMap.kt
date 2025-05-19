package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.FlowCollector

/**
 * 요소를 병렬로 매핑합니다.
 */
internal class FlowParallelMap<T, R>(
    private val source: ParallelFlow<T>,
    private val mapper: suspend (T) -> R,
): ParallelFlow<R> {

    companion object: KLoggingChannel()

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism
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
