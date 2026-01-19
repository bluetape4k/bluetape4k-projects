package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.exceptions.FlowNoElementException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * Single collector가 collect 하기 전까지는 버퍼링을 합니다.
 *
 * **collector 가 완료되면, 재작업을 수행할 수 없습니다.**
 *
 * ```
 * val us = UnicastSubject<Int>()
 * repeat(5) {
 *     us.emit(it)
 * }
 * us.complete()
 * val result = us.log("#1").toList()
 * result shouldBeEqualTo expectedList
 * us.collectorCancelled.shouldBeTrue()
 * ```
 */
class UnicastSubject<T: Any>: AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val terminatedCollector = FlowCollector<Any?> {
            log.trace { "TerminatedCollector was called." }
        }
        private val terminated = FlowNoElementException("No more elements")
    }

    val resumable = Resumable()

    private val queue = ConcurrentLinkedQueue<T>()
    private var terminal by atomic<Throwable?>(null)

    private val current = atomic<FlowCollector<T>?>(null)

    val collectorCancelled: Boolean
        get() = current.value == terminatedCollector

    override val hasCollectors: Boolean
        get() = current.value != null && current.value != terminatedCollector

    override val collectorCount: Int
        get() = if (hasCollectors) 1 else 0

    override suspend fun collectSafely(collector: FlowCollector<T>) {
        while (true) {
            val curr = current.value
            if (curr != null) {
                error("Only one collector allowed.")
            }
            if (current.compareAndSet(curr, collector)) {
                break
            }
        }

        while (true) {
            val t = terminal
            val v = queue.poll()

            // 종료되었거나 요소가 없을 때
            if (t != null && v == null) {
                current.getAndSet(terminatedCollector as FlowCollector<T>)
                if (t != terminated) {
                    throw t
                }
                return
            }
            if (v != null) {
                try {
                    collector.emit(v)
                } catch (e: Throwable) {
                    current.getAndSet(terminatedCollector as FlowCollector<T>)
                    queue.clear()
                    throw e
                }
            } else {
                resumable.await()
            }
        }
    }

    override suspend fun emit(value: T) {
        if (current.value != terminatedCollector) {
            queue.offer(value)
            resumable.resume()
        } else {
            queue.clear()
        }
    }

    override suspend fun emitError(ex: Throwable?) {
        terminal = ex
        resumable.resume()
    }

    override suspend fun complete() {
        terminal = terminated
        resumable.resume()
    }
}
