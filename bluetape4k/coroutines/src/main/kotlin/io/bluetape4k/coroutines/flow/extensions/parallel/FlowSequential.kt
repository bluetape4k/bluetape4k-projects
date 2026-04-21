package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal class FlowSequential<T>(private val source: ParallelFlow<T>): AbstractFlow<T>() {

    companion object: KLoggingChannel()

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val n = source.parallelism
            val out = Channel<T>(capacity = 256)
            val writers = Array<FlowCollector<T>>(n) { ChannelWriter(out) }

            val producer = launch {
                try {
                    source.collect(*writers)
                    out.close()
                } catch (ex: Throwable) {
                    out.close(ex)
                }
            }

            try {
                for (v in out) {
                    coroutineContext.ensureActive()
                    collector.emit(v)
                }
            } catch (ex: Throwable) {
                producer.cancel()
                throw ex
            }
        }
    }

    private class ChannelWriter<T>(private val channel: SendChannel<T>) : FlowCollector<T> {
        override suspend fun emit(value: T) {
            channel.send(value)
        }
    }
}
