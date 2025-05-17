package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.coroutines.flow.extensions.ResumableCollector
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector


/**
 * 특정 수의 `collectors` 가 consume 을 시작할 때까지 producer는 대기합니다.
 *
 * ```
 * val subject = MulticastSubject<Int>(1)
 * val result = CopyOnWriteArrayList<Int>()
 * withSingleThread { dispatcher ->
 *     val job = launch(dispatcher) {
 *         subject
 *             .onEach { delay(10) }
 *             .log("#1")
 *             .collect { result.add(it) }
 *     }.log("job")
 *     // collector가 등록되어 실행될 때까지 대기합니다.
 *     subject.awaitCollector()
 *     repeat(10) {
 *         subject.emit(it)
 *     }
 *     subject.complete()
 *     job.join()
 * }
 * result shouldBeEqualTo List(10) { it }
 * ```
 *
 * @param <T> the element type of the [Flow]
 * @param expectedCollectorSize 기대하는 collector 수. 이 수만큼 collector가 등록되어야 producer가 작동합니다.
 */
class MulticastSubject<T> private constructor(
    expectedCollectorSize: Int,
): AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val EMPTY = arrayOf<ResumableCollector<Any>>()
        private val TERMINATED = arrayOf<ResumableCollector<Any>>()
        private val DONE = FlowOperationException("Subject completed")

        operator fun <T> invoke(expectedCollectorSize: Int): MulticastSubject<T> {
            return MulticastSubject(expectedCollectorSize.coerceAtLeast(1))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val collectorsRef = atomic(EMPTY as Array<ResumableCollector<T>>)
    private val collectors by collectorsRef

    private val producer = Resumable()

    private val remainingCollectors = atomic(expectedCollectorSize)

    @Volatile
    private var terminated: Throwable? = null

    override val hasCollectors: Boolean
        get() = collectors.isNotEmpty()

    override val collectorCount: Int
        get() = collectors.size

    override suspend fun emit(value: T) {
        awaitCollectors()
        collectors.forEach { collector ->
            try {
                collector.next(value)
            } catch (e: CancellationException) {
                remove(collector)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun emitError(ex: Throwable?) {
        terminated = ex
        collectorsRef.getAndSet(TERMINATED as Array<ResumableCollector<T>>).forEach { collector ->
            try {
                collector.error(ex)
            } catch (_: CancellationException) {
                // ignored at this point
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun complete() {
        terminated = DONE
        collectorsRef.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
            .forEach { collector ->
                try {
                    collector.complete()
                } catch (_: CancellationException) {
                    log.debug { "$collector is cancelled." }
                }
            }
    }

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val rc = ResumableCollector<T>()
        if (add(rc)) {
            while (true) {
                val a = remainingCollectors.value
                if (a == 0) {
                    break
                }
                if (remainingCollectors.compareAndSet(a, a - 1)) {
                    if (a == 1) {
                        producer.resume()
                    }
                    break
                }
            }
            rc.drain(collector) { remove(it) }
        } else {
            val ex = terminated
            if (ex != null && ex != DONE) {
                throw ex
            }
        }
    }

    private suspend fun awaitCollectors() {
        if (remainingCollectors.value > 0) {
            producer.await()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun add(inner: ResumableCollector<T>): Boolean {
        while (true) {
            val array = collectors
            if (areEqualAsAny(array, TERMINATED)) {
                return false
            }
            val n = array.size
            val b = array.copyOf(n + 1)
            b[n] = inner
            if (collectorsRef.compareAndSet(array, b as Array<ResumableCollector<T>>)) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun remove(inner: ResumableCollector<T>) {
        while (true) {
            val array = collectors
            val n = array.size
            if (n == 0) {
                return
            }

            val j = array.indexOf(inner)
            if (j < 0) {
                return
            }

            var b = EMPTY as Array<ResumableCollector<T>?>
            if (n != 1) {
                b = Array(n - 1) { null }
                array.copyInto(b, 0, 0, j)
                array.copyInto(b, j, j + 1)
            }
            if (collectorsRef.compareAndSet(array, b as Array<ResumableCollector<T>>)) {
                return
            }
        }
    }
}
