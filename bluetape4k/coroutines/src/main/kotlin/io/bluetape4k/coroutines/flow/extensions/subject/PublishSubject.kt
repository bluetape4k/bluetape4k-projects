package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.ResumableCollector
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.CancellationException


/**
 * 등록된 복수의 Collectors 에게 수신한 items 를 multicast 합니다.
 *
 * NOTE: [kotlinx.coroutines.flow.SharedFlow] 를 사용하는 걸 추천합니다.
 *
 * ```
 * withSingleThread { dispatcher ->
 *     val subject = PublishSubject<Int>()
 *     val n = 10_000
 *     val counter = atomic(0)
 *     val count by counter
 *     val job = launch(dispatcher) {
 *         subject.collect {
 *             counter.incrementAndGet()
 *         }
 *     }
 *     subject.awaitCollectors(1)
 *     repeat(n) { i ->
 *         subject.emit(i)
 *     }
 *     subject.complete()
 *     job.join()
 *     count shouldBeEqualTo n
 * }
 * ```
 *
 * @see [kotlinx.coroutines.flow.SharedFlow]
 */
@Suppress("UNCHECKED_CAST")
class PublishSubject<T>: AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLogging() {
        private val EMPTY = arrayOf<ResumableCollector<Any>>()
        private val TERMINATED = arrayOf<ResumableCollector<Any>>()
    }

    private val _collectors = atomic(EMPTY as Array<ResumableCollector<T>>)
    private val collectors by _collectors

    private var error: Throwable? = null

    override val hasCollectors: Boolean
        get() = collectors.isNotEmpty()

    override val collectorCount: Int
        get() = collectors.size


    /**
     * Start collecting signals from this PublishSubject.
     */
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val inner = ResumableCollector<T>()
        if (add(inner)) {
            // inner 에 있는 item을 collector 에게 전달합니다.
            inner.drain(collector) { this.remove(it) }
            return
        }
        error?.let { throw it }
    }

    /**
     * Emit the value to all current collectors, waiting for each of them to be ready for consuming it.
     */
    override suspend fun emit(value: T) {
        // 등록된 모든 collector 에게 value 를 전달합니다.
        collectors.forEach { collector ->
            try {
                collector.next(value)
            } catch (e: CancellationException) {
                remove(collector)
            }
        }
    }

    /**
     * Throw an error on the consumer side.
     */
    override suspend fun emitError(ex: Throwable?) {
        if (this.error == null) {
            this.error = ex
            val colls = _collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
            colls.forEach { collector ->
                runCatching { collector.error(ex) }
            }
        }
    }

    /**
     * Indicate no further items will be emitted
     */
    override suspend fun complete() {
        val colls = _collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
        colls.forEach { collector ->
            runCatching { collector.complete() }
        }
    }


    private fun add(inner: ResumableCollector<T>): Boolean {
        while (true) {
            val a = collectors
            if (areEqualAsAny(a, TERMINATED)) {
                return false
            }
            val n = a.size
            val b = a.copyOf(n + 1)
            b[n] = inner
            if (_collectors.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return true
            }
        }
    }

    private fun remove(inner: ResumableCollector<T>) {
        while (true) {
            val a = collectors
            val n = a.size
            if (n == 0) {
                return
            }

            val j = a.indexOf(inner)
            if (j < 0) {
                return
            }

            var b = EMPTY as Array<ResumableCollector<T>?>
            if (n != 1) {
                b = Array(n - 1) { null }
                a.copyInto(b, 0, 0, j)
                a.copyInto(b, j, j + 1)
            }
            if (_collectors.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return
            }
        }
    }
}
