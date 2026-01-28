package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


/**
 * Producer 가 emit 한 item 들을 캐시하고, collector 가 소비할 때 replay 방식으로 요소를 제공한다
 *
 * NOTE: [kotlinx.coroutines.flow.SharedFlow] 를 사용하는 걸 추천합니다.
 *
 * ```
 * val replay = ReplaySubject<Int>()
 * repeat(5) {
 *     replay.emit(it)
 * }
 * replay.complete()
 * val result = mutableListOf<Int>()
 * replay
 *     .onEach { delay(10) }
 *     .log("#1")
 *     .collect { result.add(it) }
 * result shouldBeEqualTo mutableListOf(0, 1, 2, 3, 4)
 * ```
 *
 * @see [kotlinx.coroutines.flow.SharedFlow]
 */
class ReplaySubject<T>: AbstractFlow<T>, SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val EMPTY = arrayOf<InnerCollector<Any>>()
        private val TERMINATED = arrayOf<InnerCollector<Any>>()
    }

    private val buffer: Buffer<T>

    @Suppress("UNCHECKED_CAST")
    private val collectors = atomic<Array<InnerCollector<T>>>(EMPTY as Array<InnerCollector<T>>)

    private val done = atomic(false)

    constructor() {
        buffer = UnboundedReplayBuffer()
    }

    constructor(maxSize: Int) {
        buffer = SizeBoundReplayBuffer(maxSize.coerceAtLeast(1))
    }

    constructor(maxTime: Long, unit: TimeUnit): this(Int.MAX_VALUE, maxTime, unit)

    constructor(maxSize: Int, maxTime: Long, unit: TimeUnit): this(
        maxSize, maxTime, unit, { it.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) })

    constructor(maxSize: Int, maxTime: Long, unit: TimeUnit, timeSource: (TimeUnit) -> Long) {
        buffer = TimeAndSizeBoundReplayBuffer(maxSize, maxTime, unit, timeSource)
    }

    override val hasCollectors: Boolean
        get() = collectors.value.isNotEmpty()

    override val collectorCount: Int
        get() = collectors.value.size


    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val inner = InnerCollector(collector, this)
        add(inner)
        buffer.replay(inner)
    }

    override suspend fun emit(value: T) {
        if (done.value) {
            return
        }
        buffer.emit(value)
        collectors.value.forEach { collector ->
            collector.resume()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun emitError(ex: Throwable?) {
        if (done.value) {
            return
        }
        done.value = true
        buffer.error(ex)

        collectors.getAndSet(TERMINATED as Array<InnerCollector<T>>).forEach { collector ->
            collector.resume()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun complete() {
        if (done.value) {
            return
        }
        done.value = true
        buffer.complete()

        collectors.getAndSet(TERMINATED as Array<InnerCollector<T>>).forEach { collector ->
            collector.resume()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun add(inner: InnerCollector<T>): Boolean {
        while (true) {
            val a = collectors.value
            if (areEqualAsAny(a, TERMINATED)) {
                return false
            }
            val n = a.size
            val b = a.copyOf(n + 1)
            b[n] = inner
            if (collectors.compareAndSet(a, b as Array<InnerCollector<T>>)) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun remove(inner: InnerCollector<T>) {
        while (true) {
            val a = collectors.value
            val n = a.size
            if (n == 0) {
                return
            }

            val j = a.indexOf(inner)
            if (j < 0) {
                return
            }

            var b = EMPTY as Array<InnerCollector<T>?>
            if (n != 1) {
                b = Array(n - 1) { null }
                a.copyInto(b, 0, 0, j)
                a.copyInto(b, j, j + 1)
            }
            if (collectors.compareAndSet(a, b as Array<InnerCollector<T>>)) {
                return
            }
        }
    }


    private interface Buffer<T> {
        fun emit(value: T)
        fun error(e: Throwable?)
        fun complete()
        suspend fun replay(consumer: InnerCollector<T>)
    }

    private class InnerCollector<T>(val consumer: FlowCollector<T>, val parent: ReplaySubject<T>): Resumable() {
        var index: Long = 0L
        var node: Any? = null
    }

    private class UnboundedReplayBuffer<T>: Buffer<T> {

        private companion object: KLoggingChannel()

        private val size = atomic(0L)
        private val list = ArrayDeque<T>()
        private val done = atomic(false)
        private val error = atomic<Throwable?>(null)

        override fun emit(value: T) {
            list.add(value)
            size.incrementAndGet()
        }

        override fun error(e: Throwable?) {
            error.value = e
            done.value = true
        }

        override fun complete() {
            done.value = true
        }

        override suspend fun replay(consumer: InnerCollector<T>) = coroutineScope {
            log.debug { "Replay emit ..." }

            while (true) {
                val d = done.value
                val empty = consumer.index == size.value
                if (d && empty) {
                    error.value?.let { throw it }
                    return@coroutineScope
                }
                if (!empty) {
                    try {
                        if (coroutineContext.isActive) {
                            consumer.consumer.emit(list[consumer.index.toInt()])
                            consumer.index++
                        } else {
                            throw CancellationException()
                        }
                    } catch (e: Throwable) {
                        consumer.parent.remove(consumer)
                        throw e
                    }
                    continue
                }
                consumer.await()
            }
        }
    }

    private class SizeBoundReplayBuffer<T>(private val maxSize: Int): Buffer<T> {

        private var size: Int = 0

        private val done = atomic(false)
        private val error = atomic<Throwable?>(null)

        @Volatile
        private var head: Node<T>

        @Volatile
        private var tail: Node<T>

        init {
            val h = Node<T>(null)
            tail = h
            head = h
        }

        override fun emit(value: T) {
            val next = Node(value)
            tail.set(next)
            tail = next

            if (size == maxSize) {
                head = head.get()
            } else {
                size++
            }
        }

        override fun error(e: Throwable?) {
            error.value = e
            done.value = true
        }

        override fun complete() {
            done.value = true
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun replay(consumer: InnerCollector<T>) = coroutineScope {
            while (true) {
                val d = done.value
                var index = consumer.node as? Node<T>
                if (index == null) {
                    index = head
                    consumer.node = index
                }
                val next = index.get()
                val empty = next == null

                if (d && empty) {
                    error.value?.let { throw it }
                    return@coroutineScope
                }
                if (!empty) {
                    try {
                        if (coroutineContext.isActive) {
                            consumer.consumer.emit(next.value!!)
                            consumer.node = next
                        } else {
                            throw CancellationException()
                        }
                    } catch (e: Throwable) {
                        consumer.parent.remove(consumer)
                        throw e
                    }
                    continue
                }
                consumer.await()
            }
        }

        private class Node<T>(val value: T?): AtomicReference<Node<T>>()
    }

    private class TimeAndSizeBoundReplayBuffer<T>(
        private val maxSize: Int,
        private val maxTime: Long,
        private val unit: TimeUnit,
        private val timeSource: (TimeUnit) -> Long,
    ): Buffer<T> {
        private var size: Int = 0

        private val done = atomic(false)
        private val error = atomic<Throwable?>(null)

        @Volatile
        private var head: Node<T>

        @Volatile
        private var tail: Node<T>

        init {
            val h = Node<T>(null, 0L)
            tail = h
            head = h
        }

        override fun emit(value: T) {
            val now = timeSource(unit)
            val next = Node(value, now)
            tail.set(next)
            tail = next

            if (size == maxSize) {
                head = head.get()
            } else {
                size++
            }
            trimTime(now)
        }

        fun trimTime(now: Long) {
            val limit = now - maxTime
            var h = head

            while (true) {
                val next = h.get()
                if (next != null && next.timestamp <= limit) {
                    h = next
                    size--
                } else {
                    break
                }
            }
            head = h
        }

        override fun error(e: Throwable?) {
            error.value = e
            done.value = true
        }

        override fun complete() {
            done.value = true
        }

        fun findHead(): Node<T> {
            val limit = timeSource(unit) - maxTime
            var h = head

            while (true) {
                val next = h.get()
                if (next != null && next.timestamp <= limit) {
                    h = next
                } else {
                    break
                }
            }
            return h
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun replay(consumer: InnerCollector<T>) = coroutineScope {
            while (true) {
                val d = done.value
                var index = consumer.node as? Node<T>
                if (index == null) {
                    index = findHead()
                    consumer.node = index
                }
                val next = index.get()
                val empty = next == null

                if (d && empty) {
                    error.value?.let { throw it }
                    return@coroutineScope
                }
                if (!empty) {
                    try {
                        if (coroutineContext.isActive) {
                            consumer.consumer.emit(next.value!!)
                            consumer.node = next
                        } else {
                            throw CancellationException()
                        }
                    } catch (e: Throwable) {
                        consumer.parent.remove(consumer)
                        throw e
                    }
                    continue
                }
                consumer.await()
            }
        }

        private class Node<T>(
            val value: T?,
            val timestamp: Long,
        ): AtomicReference<Node<T>>()
    }
}
