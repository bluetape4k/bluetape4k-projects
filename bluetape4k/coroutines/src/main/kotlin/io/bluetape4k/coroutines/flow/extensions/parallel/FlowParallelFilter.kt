package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.FlowCollector

/**
 * 병렬로 flow 요소를 필터링합니다.
 */
internal class FlowParallelFilter<T>(
    private val source: ParallelFlow<T>,
    private val predicate: suspend (T) -> Boolean,
): ParallelFlow<T> {

    companion object: KLoggingChannel()

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<T>) {
        val n = parallelism
        val rails = Array(n) { FilterCollector(collectors[it], predicate) }

        source.collect(*rails)
    }

    private class FilterCollector<T>(
        val collector: FlowCollector<T>,
        val predicate: suspend (T) -> Boolean,
    ): FlowCollector<T> {
        override suspend fun emit(value: T) {
            if (predicate(value)) {
                collector.emit(value)
            }
        }
    }
}
