package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * [source]의 요소를 주어진 수의 병렬 collector 에 라운드 로빈 방식으로 전달합니다.
 */
internal class FlowParallel<T>(
    private val source: Flow<T>,
    override val parallelism: Int,
    private val runOn: (Int) -> CoroutineDispatcher,
): ParallelFlow<T> {

    companion object: KLoggingChannel()

    override suspend fun collect(vararg collectors: FlowCollector<T>) {
        coroutineScope {
            val n = collectors.size
            if (n != parallelism) {
                throw IllegalArgumentException("Wrong number of collectors. Expected: $parallelism, Actual: ${collectors.size}")
            }

            val generator = Resumable()
            val rails = Array<RailCollector<T>>(parallelism) { RailCollector(generator) }

            for (i in 0 until n) {
                launch(runOn(i)) {
                    rails[i].drain(collectors[i])
                }
            }

            val index = AtomicInteger(0)

            try {
                source.collect {
                    var idx = index.get()

                    outer@ while (true) {
                        for (i in 0 until n) {
                            val j = idx
                            val rail = rails[j]
                            idx = j + 1
                            if (idx == n) {
                                idx = 0
                            }
                            if (rail.next(it)) {
                                index.lazySet(idx)
                                break@outer
                            }
                        }
                        index.lazySet(idx)
                        generator.await()
                    }
                }
                for (rail in rails) {
                    rail.complete()
                }
            } catch (ex: Throwable) {
                for (rail in rails) {
                    rail.error(ex)
                }
            }
        }
    }

    class RailCollector<T>(private val resumeGenerator: Resumable): Resumable() {

        private val consumerReady = atomic(false)

        @Suppress("UNCHECKED_CAST")
        private var value: T = null as T

        @Volatile
        private var hasValue: Boolean = false

        @Volatile
        private var error: Throwable? = null

        @Volatile
        private var done: Boolean = false

        fun next(value: T): Boolean {
            if (consumerReady.compareAndSet(expect = true, update = false)) {
                this.value = value
                this.hasValue = true
                resume()
                return true
            }
            return false
        }

        fun error(ex: Throwable) {
            this.error = ex
            this.done = true
            resume()
        }

        fun complete() {
            this.done = true
            resume()
        }

        suspend fun drain(collector: FlowCollector<T>) {
            while (true) {
                consumerReady.value = true
                resumeGenerator.resume()

                await()

                if (hasValue) {
                    val v = value
                    @Suppress("UNCHECKED_CAST")
                    value = null as T
                    hasValue = false

                    try {
                        collector.emit(v)
                    } catch (ex: Throwable) {
                        resumeGenerator.resume()
                        throw ex
                    }
                }

                if (done) {
                    if (error != null) {
                        throw FlowOperationException("Fail to drain", error)
                    }
                    return
                }
            }
        }
    }
}
