package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.FlowCollector

/**
 * [source] 요소를 복수의 collector에게 병렬로 emit합니다.
 */
internal class FlowParallelTransform<T, R>(
    private val source: ParallelFlow<T>,
    private val callback: suspend FlowCollector<R>.(T) -> Unit,
): ParallelFlow<R> {

    companion object: KLoggingChannel()

    override val parallelism: Int
        get() = source.parallelism

    override suspend fun collect(vararg collectors: FlowCollector<R>) {
        val n = parallelism
        val rails = Array(n) { OnEachCollector(collectors[it], callback) }

        source.collect(*rails)
    }

    class OnEachCollector<T, R>(
        val collector: FlowCollector<R>,
        val callback: suspend FlowCollector<R>.(T) -> Unit,
    ): FlowCollector<T> {
        override suspend fun emit(value: T) {
            callback(collector, value)
        }
    }
}
