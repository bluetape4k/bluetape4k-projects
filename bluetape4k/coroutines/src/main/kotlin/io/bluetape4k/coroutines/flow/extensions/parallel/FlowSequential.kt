package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

            val done = AtomicBoolean(false)
            val error = AtomicReference<Throwable?>(null)

            launch {
                try {
                    source.collect(*collectors)

                    done.set(true)
                    resumeCollector.resume()
                } catch (ex: Throwable) {
                    error.set(ex)
                    done.set(true)
                    resumeCollector.resume()
                }
            }

            while (true) {
                val d = done.get()
                var empty = true

                for (rail in collectors) {
                    if (rail.hasValue) {
                        empty = false
                        val v = rail.value
                        @Suppress("UNCHECKED_CAST")
                        rail.value = null as T
                        rail.hasValue = false

                        try {
                            collector.emit(v)
                        } catch (ex: Throwable) {
                            for (r in collectors) {
                                r.error = ex
                                r.resume()
                            }
                            throw ex
                        }
                        rail.resume()
                        break
                    }
                }

                if (d && empty) {
                    val ex = error.get()
                    if (ex != null) {
                        throw ex
                    }
                    return@coroutineScope
                }
                if (empty) {
                    resumeCollector.await()
                }
            }
        }
    }

    class RailCollector<T>(private val resumeCollector: Resumable): Resumable(), FlowCollector<T> {

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

            if (error != null) {
                throw FlowOperationException("Error occurred while emitting value. $value", error)
            }
        }
    }
}
