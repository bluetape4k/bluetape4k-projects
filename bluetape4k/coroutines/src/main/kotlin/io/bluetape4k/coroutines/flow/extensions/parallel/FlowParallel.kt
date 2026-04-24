package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

internal class FlowParallel<T>(
    private val source: Flow<T>,
    override val parallelism: Int,
    private val runOn: (Int) -> CoroutineDispatcher,
): ParallelFlow<T> {

    companion object: KLoggingChannel()

    override suspend fun collect(vararg collectors: FlowCollector<T>) {
        coroutineScope {
            val n = collectors.size
            n.requireEquals(parallelism, "collectors.size")

            val rails: Array<Channel<T>> = Array(n) { Channel(capacity = 256) }

            for (i in 0 until n) {
                launch(runOn(i)) {
                    rails[i].consumeEach { v -> collectors[i].emit(v) }
                }
            }

            try {
                var idx = 0
                source.collect { v ->
                    rails[idx].send(v)
                    idx++
                    if (idx == n) idx = 0
                }
                rails.forEach { it.close() }
            } catch (ex: Throwable) {
                rails.forEach { it.close(ex) }
                throw ex
            }
        }
    }
}
