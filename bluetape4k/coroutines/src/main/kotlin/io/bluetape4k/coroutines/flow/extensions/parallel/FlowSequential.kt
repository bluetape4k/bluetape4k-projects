package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

/**
 * 병렬 flow인 [source]를 소비하고 값을 순차적인 Flow 로 변환합니다.
 */
internal class FlowSequential<T>(private val source: ParallelFlow<T>): AbstractFlow<T>() {

    companion object: KLoggingChannel()

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val n = source.parallelism
            val resumeCollector = Resumable()
            val collectors = Array(n) { RailCollector<T>(resumeCollector) }
            val state = FlowSequentialState()

            launch {
                try {
                    source.collect(*collectors)
                    state.done.value = true
                    resumeCollector.resume()
                } catch (ex: Throwable) {
                    state.error.value = ex
                    state.done.value = true
                    resumeCollector.resume()
                }
            }

            while (true) {
                val d = state.done.value
                var empty = true

                collectors.forEach { rail ->
                    if (rail.hasValue) {
                        empty = false
                        val v = rail.value

                        @Suppress("UNCHECKED_CAST")
                        rail.value = null as T
                        rail.hasValue = false

                        try {
                            collector.emit(v)
                        } catch (ex: Throwable) {
                            collectors.forEach {
                                it.error = ex
                                it.resume()
                            }
                            throw ex
                        }
                        rail.resume()
                        return@forEach
                    }
                }

                if (d && empty) {
                    val ex = state.error.value
                    ex?.let { throw it }
                    return@coroutineScope
                }
                if (empty) {
                    resumeCollector.await()
                }
            }
        }
    }

    /**
     * 병렬 source 종료 여부와 종료 예외를 보관합니다.
     */
    private class FlowSequentialState {
        val done = atomic(false)
        val error = atomic<Throwable?>(null)
    }

    private class RailCollector<T>(private val resumeCollector: Resumable): Resumable(), FlowCollector<T> {

        var value: T = uninitialized()

        @Volatile
        var hasValue: Boolean = false

        @Volatile
        var error: Throwable? = null

        override suspend fun emit(value: T) {
            this.value = value
            hasValue = true
            resumeCollector.resume()

            await()

            error?.let {
                throw FlowOperationException("Error occurred while emitting value. $value", it)
            }
        }
    }
}
