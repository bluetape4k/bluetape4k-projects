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


/**
 * 과거에 emit된 값을 새 구독자에게 다시 재생(replay)하는 Subject입니다.
 *
 * ## 동작/계약
 * - 버퍼 정책은 생성자에 따라 무제한/크기 제한/시간+크기 제한으로 결정됩니다.
 * - `complete()` 또는 `emitError()` 이후에는 새 값 emit을 무시하고 종료 상태를 유지합니다.
 * - 새 수집자는 버퍼에 남아 있는 과거 값을 먼저 받고, 이후 실시간 값을 이어서 받습니다.
 * - 같은 collector 배열을 원자적으로 교체하며, 버퍼 크기에 비례한 추가 메모리 할당이 발생할 수 있습니다.
 *
 * ```kotlin
 * val subject = ReplaySubject<Int>(5)
 * subject.emit(1); subject.emit(2); subject.complete()
 * // 이후 새 collect 시작 시 1, 2를 먼저 replay 받음
 * ```
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

    /**
     * 무제한 버퍼 replay subject를 생성합니다.
     *
     * ## 동작/계약
     * - 모든 emit 값을 제한 없이 저장합니다.
     * - 늦게 구독한 수집자도 누적된 전체 값을 replay 받습니다.
     * - 장시간 사용 시 메모리 사용량이 계속 증가할 수 있습니다.
     *
     * ```kotlin
     * val subject = ReplaySubject<Int>()
     * // 버퍼 크기 제한 없음
     * ```
     */
    constructor() {
        buffer = UnboundedReplayBuffer()
    }

    /**
     * 최대 개수 제한 버퍼 replay subject를 생성합니다.
     *
     * ## 동작/계약
     * - 최근 [maxSize]개 값만 유지합니다.
     * - `maxSize <= 0`이면 내부적으로 1로 보정됩니다.
     * - 새 수집자는 보정된 버퍼 범위 내 최근 값만 replay 받습니다.
     *
     * ```kotlin
     * val subject = ReplaySubject<Int>(3)
     * // 최근 3개 값만 replay
     * ```
     * @param maxSize 유지할 최대 값 개수입니다.
     */
    constructor(maxSize: Int) {
        buffer = SizeBoundReplayBuffer(maxSize.coerceAtLeast(1))
    }

    /**
     * 시간 제한(무제한 크기) replay subject를 생성합니다.
     *
     * ## 동작/계약
     * - 시간 윈도우 안의 값만 replay 대상으로 유지합니다.
     * - 실제 구현은 크기 제한 `Int.MAX_VALUE`와 시간 제한을 함께 적용합니다.
     * - `maxTime < 0`이면 내부적으로 0으로 보정됩니다.
     *
     * ```kotlin
     * val subject = ReplaySubject<Int>(1_000L, TimeUnit.MILLISECONDS)
     * // 최근 1초 이내 값만 replay
     * ```
     * @param maxTime replay 보존 시간입니다.
     * @param unit [maxTime]의 단위입니다.
     */
    constructor(maxTime: Long, unit: TimeUnit): this(Int.MAX_VALUE, maxTime, unit)

    /**
     * 크기+시간 제한 replay subject를 생성합니다.
     *
     * ## 동작/계약
     * - 최근 [maxSize]개와 [maxTime]/[unit] 시간 조건을 동시에 만족하는 값만 유지합니다.
     * - `maxSize <= 0`은 1로, `maxTime < 0`은 0으로 보정됩니다.
     * - 시간 기준은 시스템 시간을 [unit]으로 환산해 사용합니다.
     *
     * ```kotlin
     * val subject = ReplaySubject<Int>(16, 1_000L, TimeUnit.MILLISECONDS)
     * // 최대 16개 + 최근 1초 내 값 replay
     * ```
     * @param maxSize 유지할 최대 값 개수입니다.
     * @param maxTime replay 보존 시간입니다.
     * @param unit [maxTime]의 단위입니다.
     */
    constructor(maxSize: Int, maxTime: Long, unit: TimeUnit): this(
        maxSize, maxTime, unit, { it.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) })

    /**
     * 크기+시간 제한 replay subject를 사용자 정의 시간 소스로 생성합니다.
     *
     * ## 동작/계약
     * - [timeSource]가 제공한 현재 시각을 기준으로 시간 만료를 계산합니다.
     * - `maxSize <= 0`은 1로, `maxTime < 0`은 0으로 보정됩니다.
     * - 테스트 환경에서 가상 시간 소스를 주입해 재현 가능한 동작을 만들 수 있습니다.
     *
     * ```kotlin
     * val subject = ReplaySubject<Int>(16, 1_000L, TimeUnit.MILLISECONDS) { 0L }
     * // 시간 소스를 고정해 replay 창 계산
     * ```
     * @param maxSize 유지할 최대 값 개수입니다.
     * @param maxTime replay 보존 시간입니다.
     * @param unit [maxTime]의 단위입니다.
     * @param timeSource 현재 시각을 반환하는 함수입니다.
     */
    constructor(maxSize: Int, maxTime: Long, unit: TimeUnit, timeSource: (TimeUnit) -> Long) {
        buffer = TimeAndSizeBoundReplayBuffer(
            maxSize.coerceAtLeast(1),
            maxTime.coerceAtLeast(0L),
            unit,
            timeSource
        )
    }

    /**
     * 현재 활성 수집자가 하나 이상인지 반환합니다.
     *
     * ## 동작/계약
     * - collector 배열 길이를 기준으로 즉시 계산합니다.
     * - 조회 전용이며 subject 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val has = subject.hasCollectors
     * // has == false || true
     * ```
     */
    override val hasCollectors: Boolean
        get() = collectors.value.isNotEmpty()

    /**
     * 현재 활성 수집자 수를 반환합니다.
     *
     * ## 동작/계약
     * - collector 배열 길이를 그대로 반환합니다.
     * - 조회 전용이며 subject 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val count = subject.collectorCount
     * // count >= 0
     * ```
     */
    override val collectorCount: Int
        get() = collectors.value.size


    /**
     * 수집자를 등록하고 버퍼 값을 replay한 뒤 실시간 값을 전달합니다.
     *
     * ## 동작/계약
     * - 수집 시작 시 현재 버퍼 상태를 먼저 replay합니다.
     * - 완료/오류 상태라면 replay 후 종료되거나 오류를 전달합니다.
     * - 수집 종료 시 등록된 수집자를 제거합니다(정상/취소 모두 포함).
     *
     * ```kotlin
     * val subject = ReplaySubject<Int>(5)
     * subject.emit(1); subject.emit(2); subject.complete()
     * // 이후 collect 시작 시 1, 2를 replay 받고 종료
     * ```
     * @param collector 값을 받을 수집자입니다.
     */
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val inner = InnerCollector(collector, this)
        val added = add(inner)
        try {
            buffer.replay(inner)
        } finally {
            if (added) {
                remove(inner)
            }
        }
    }

    /**
     * 값을 버퍼에 저장하고 현재 수집자들에게 전달합니다.
     *
     * ## 동작/계약
     * - 완료/오류 이후 호출은 무시됩니다.
     * - 활성 수집자는 즉시 resume되어 새 값을 소비합니다.
     * - 버퍼 정책에 따라 과거 값 보존 범위가 달라집니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.emit(2)
     * // 이후 새 collect 시작 시 정책 범위 내 값 replay
     * ```
     * @param value 방출할 값입니다.
     */
    override suspend fun emit(value: T) {
        if (done.value) {
            return
        }
        buffer.emit(value)
        collectors.value.forEach { collector ->
            collector.resume()
        }
    }

    /**
     * subject를 오류 종료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 최초 한 번만 종료 전환이 적용되고 이후 호출은 무시됩니다.
     * - 현재 수집자는 resume되어 replay 이후 오류를 받습니다.
     * - 이후 새 수집자도 버퍼 replay 후 같은 오류를 받습니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.emitError(IllegalStateException(\"boom\"))
     * // collect 시 replay 후 예외 전달
     * ```
     * @param ex 종료 원인 예외입니다.
     */
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

    /**
     * subject를 정상 완료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 최초 한 번만 완료 전환이 적용되고 이후 호출은 무시됩니다.
     * - 현재 수집자는 resume되어 남은 replay를 받고 정상 종료합니다.
     * - 이후 새 수집자도 버퍼 replay 후 정상 종료합니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.complete()
     * // 이후 새 collect 시 1 replay 후 완료
     * ```
     */
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
                head = requireNotNull(head.get())
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
                        throw e
                    }
                    continue
                }
                consumer.await()
            }
        }

        private class Node<T>(val value: T?) {
            private val next = atomic<Node<T>?>(null)

            fun get(): Node<T>? = next.value

            fun set(node: Node<T>) {
                next.value = node
            }
        }
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
                head = requireNotNull(head.get())
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
        ) {
            private val next = atomic<Node<T>?>(null)

            fun get(): Node<T>? = next.value

            fun set(node: Node<T>) {
                next.value = node
            }
        }
    }
}
