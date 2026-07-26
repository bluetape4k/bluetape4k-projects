package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

internal class FlowSequential<T>(private val source: ParallelFlow<T>): AbstractFlow<T>() {

    companion object: KLoggingChannel()

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val n = source.parallelism

            // Per-rail channels. Each rail writes to its own channel instead of
            // contending on a single shared output channel. The consumer uses
            // `select` to fairly multiplex across the N channels.
            val perRailChannels = Array(n) { Channel<T>(capacity = 64) }
            val writers = Array<FlowCollector<T>>(n) { i -> ChannelWriter(perRailChannels[i]) }

            val producer = launch {
                try {
                    source.collect(*writers)
                    perRailChannels.forEach { it.close() }
                } catch (ex: Throwable) {
                    perRailChannels.forEach { it.close(ex) }
                }
            }

            try {
                // Keep a mutable list of still-open rails. When a rail closes we remove it.
                val activeChannels = perRailChannels.toMutableList()
                while (activeChannels.isNotEmpty()) {
                    // select returns either an emitted value (Result.success) or
                    // the closed marker (Result.failure or closed) per channel.
                    var receivedValue: T? = null
                    var receivedValuePresent = false
                    var closedChannel: Channel<T>? = null

                    select<Unit> {
                        for (ch in activeChannels) {
                            ch.onReceiveCatching { result ->
                                if (result.isClosed) {
                                    // Propagate any exception from the producer.
                                    val cause = result.exceptionOrNull()
                                    if (cause != null) throw cause
                                    closedChannel = ch
                                } else {
                                    receivedValue = result.getOrThrow()
                                    receivedValuePresent = true
                                }
                            }
                        }
                    }

                    if (receivedValuePresent) {
                        coroutineContext.ensureActive()
                        @Suppress("UNCHECKED_CAST")
                        collector.emit(receivedValue as T)
                    } else if (closedChannel != null) {
                        activeChannels.remove(closedChannel)
                    }
                }
            } catch (ex: Throwable) {
                producer.cancel()
                throw ex
            }
        }
    }

    private class ChannelWriter<T>(private val channel: SendChannel<T>): FlowCollector<T> {
        override suspend fun emit(value: T) {
            channel.send(value)
        }
    }
}
