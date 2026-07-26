package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 가장 최근 값을 유지하고 새 collector에게 즉시 전달하는 Subject입니다.
 *
 * ## 동작/계약
 * - 새 collector는 구독 즉시 최신 값(있다면)을 먼저 받고 이후 실시간 값을 받습니다.
 * - 기본 생성 시 초기값이 없고, `invoke(initialValue)`로 초기값을 줄 수 있습니다.
 * - `emit`/`complete`/`emitError`는 Subject 단위로 직렬화됩니다.
 * - `complete()`/`emitError()` 이후에는 종료 상태가 되며 이후 emit은 무시됩니다.
 * - 다중 collector를 허용하며 collector별 동기화 객체를 유지하므로 collector 수에 비례한 할당이 발생합니다.
 *
 * ```kotlin
 * val subject = BehaviorSubject(0)
 * subject.emit(1)
 * // 새 collector는 1을 즉시 받음
 * ```
 */
class BehaviorSubject<T> private constructor(
    @Volatile private var current: Node<T>,
): AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val EMPTY = arrayOf<InnerCollector>()
        private val TERMINATED = arrayOf<InnerCollector>()
        private val NONE = Any()
        private val DONE = Node(NONE)

        /**
         * 초기값 유무를 선택해 [BehaviorSubject]를 생성합니다.
         *
         * ## 동작/계약
         * - `initialValue`를 주면 새 collector가 구독 시 즉시 해당 값을 받습니다.
         * - 인자를 생략하면 초기값이 없는 상태로 시작합니다.
         * - 반환 인스턴스는 다중 collector를 허용합니다.
         *
         * ```kotlin
         * val subject = BehaviorSubject(0)
         * // subject.value == 0
         * ```
         * @param initialValue 초기 최신값입니다. 생략 시 초기값 없음으로 시작합니다.
         */
        // 안전: NONE은 Any 타입 sentinel이므로 실제 T 값이 아님. invoke() 호출 시 인자가 없으면
        // NONE이 전달되어 초기값 없음을 나타내며, T=Any? 조건에서 런타임 타입 소거로 안전함.
        @Suppress("UNCHECKED_CAST")
        operator fun <T: Any> invoke(initialValue: T = NONE as T): BehaviorSubject<T> {
            return BehaviorSubject(Node(initialValue))
        }
    }

    private val collectors = atomic<Array<InnerCollector>>(EMPTY)

    private val signalMutex = Mutex()

    @Volatile
    private var error: Throwable? = null

    /**
     * 현재 최신 값을 반환합니다.
     *
     * ## 동작/계약
     * - 초기값 없이 아직 값이 emit되지 않았다면 예외를 던집니다.
     * - 값이 있으면 마지막으로 emit된 값을 반환합니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val subject = BehaviorSubject(0)
     * val v = subject.value
     * // v == 0
     * ```
     */
    val value: T get() = valueOrNull ?: error("No value")

    /**
     * 현재 최신 값을 반환하고, 값이 없으면 `null`을 반환합니다.
     *
     * ## 동작/계약
     * - 초기값이 없고 아직 emit이 없으면 `null`입니다.
     * - 값이 있으면 마지막으로 emit된 값을 반환합니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val subject = BehaviorSubject<Int>()
     * val v = subject.valueOrNull
     * // v == null
     * ```
     */
    val valueOrNull: T?
        get() {
            val currentValue = current.value
            return if (currentValue == NONE) null else currentValue
        }

    /**
     * 현재 활성 collector가 하나 이상인지 반환합니다.
     *
     * ## 동작/계약
     * - 내부 collector 배열이 비어 있지 않으면 `true`입니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val has = subject.hasCollectors
     * // has == false || true
     * ```
     */
    override val hasCollectors: Boolean get() = collectors.value.isNotEmpty()

    /**
     * 현재 활성 collector 수를 반환합니다.
     *
     * ## 동작/계약
     * - 등록된 collector 배열 크기를 그대로 반환합니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val count = subject.collectorCount
     * // count >= 0
     * ```
     */
    override val collectorCount: Int get() = collectors.value.size

    /**
     * 새 값을 최신값으로 저장하고 활성 collector들에게 전달합니다.
     *
     * ## 동작/계약
     * - 종료 상태(`DONE`)면 호출을 무시합니다.
     * - 동시 호출은 Subject 단위로 직렬화해 linked node와 collector 알림 순서를 보존합니다.
     * - 최신 노드를 교체한 뒤 collector들을 깨워 새 값을 전달합니다.
     * - 상태 반영 후 producer가 취소되더라도 아직 알리지 못한 collector에는 wake-up을 남깁니다.
     * - collector가 취소되면 내부 목록에서 제거합니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.emit(2)
     * // 최신값은 2
     * ```
     * @param value 방출할 값입니다.
     */
    override suspend fun emit(value: T) = signalMutex.withLock {
        if (current == DONE) {
            return@withLock
        }

        val next = Node(value)
        current.set(next)
        current = next

        notifyCollectors(collectors.value)
    }

    /**
     * 오류 종료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 진행 중인 값 전파와 다른 종료 신호가 끝날 때까지 Subject 단위로 대기합니다.
     * - 최초 종료 전환 시 error를 저장하고 `DONE` 노드로 마킹합니다.
     * - 활성 collector를 모두 깨워 종료 경로로 진행시킵니다.
     * - 종료 상태 반영 후 호출자가 취소돼도 아직 알리지 못한 collector에는 wake-up을 남깁니다.
     * - 이후 emit/complete/emitError 호출은 무시됩니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.emitError(RuntimeException("boom"))
     * // collector는 1 이후 예외를 받음
     * ```
     * @param ex 종료 원인 예외입니다.
     */
    // 안전: DONE은 Node<Any> 타입 sentinel이며, 실제 값을 가리키지 않으므로
    // Node<T>로 캐스팅해도 런타임에서 값에 접근하지 않는 한 ClassCastException이 발생하지 않음.
    @Suppress("UNCHECKED_CAST")
    override suspend fun emitError(ex: Throwable?) = signalMutex.withLock {
        if (current == DONE) {
            return@withLock
        }

        error = ex
        current.set(DONE as Node<T>)
        current = DONE

        notifyCollectors(collectors.getAndSet(TERMINATED))
    }

    /**
     * 정상 완료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 진행 중인 값 전파와 다른 종료 신호가 끝날 때까지 Subject 단위로 대기합니다.
     * - 최초 종료 전환 시 `DONE` 노드로 마킹하고 collector를 모두 깨웁니다.
     * - collector는 남은 값을 처리한 뒤 정상 종료합니다.
     * - 종료 상태 반영 후 호출자가 취소돼도 아직 알리지 못한 collector에는 wake-up을 남깁니다.
     * - 이후 emit/complete/emitError 호출은 무시됩니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.complete()
     * // collector는 1 이후 정상 완료
     * ```
     */
    // 안전: DONE sentinel을 Node<T>로 캐스팅. sentinel의 value에 접근하지 않으므로 ClassCastException 없음.
    @Suppress("UNCHECKED_CAST")
    override suspend fun complete() = signalMutex.withLock {
        if (current == DONE) {
            return@withLock
        }

        current.set(DONE as Node<T>)
        current = DONE

        notifyCollectors(collectors.getAndSet(TERMINATED))
    }


    /**
     * collector를 등록하고 최신값/후속값을 순차 전달합니다.
     *
     * ## 동작/계약
     * - 등록 성공 시 현재 최신값이 있으면 즉시 한 번 전달합니다.
     * - 이후 링크드 노드를 따라 새 값을 전달하고, `DONE`에 도달하면 종료합니다.
     * - 종료 원인 error가 있으면 해당 예외를 던지고 종료합니다.
     * - collector 취소/예외 시 finally에서 collector를 제거합니다.
     *
     * ```kotlin
     * val result = mutableListOf<Int>()
     * launch { subject.collect { result += it } }
     * // result는 최신값부터 누적
     * ```
     * @param collector 값을 수집할 collector입니다.
     */
    override suspend fun collectSafely(collector: FlowCollector<T>) = coroutineScope<Unit> {
        val inner = InnerCollector()

        suspend fun tryEmit(isActive: Boolean, value: T) {
            try {
                if (isActive) {
                    collector.emit(value)
                } else {
                    throw CancellationException()
                }
            } catch (e: Throwable) {
                inner.consumeReady.resume()
                throw e
            }
        }

        if (add(inner)) {
            try {
                var curr = current
                if (curr.value != NONE) {
                    tryEmit(coroutineContext.isActive, curr.value)
                }

                while (true) {
                    coroutineContext.ensureActive()
                    inner.consumeReady.resume()
                    inner.await()

                    val next = curr.get() ?: continue

                    if (next == DONE) {
                        val ex = error
                        ex?.let { throw it }
                        return@coroutineScope
                    }

                    tryEmit(coroutineContext.isActive, next.value)

                    curr = next
                }
            } finally {
                remove(inner)
            }
        }

        error?.let { throw it }
    }

    private suspend fun notifyCollectors(snapshot: Array<InnerCollector>) {
        var notified = 0
        try {
            while (notified < snapshot.size) {
                val inner = snapshot[notified]
                inner.consumeReady.await()
                inner.resume()
                notified++
            }
        } finally {
            while (notified < snapshot.size) {
                snapshot[notified].resume()
                notified++
            }
        }
    }

    // 안전: TERMINATED sentinel(Array<InnerCollector>)과 실제 배열은 동일 타입이므로
    // copyOf/compareAndSet에서 런타임 타입 소거로 안전하게 캐스팅됨.
    @Suppress("UNCHECKED_CAST")
    private fun add(inner: InnerCollector): Boolean {
        while (true) {
            val a = collectors.value
            if (areEqualAsAny(a, TERMINATED)) {
                return false
            }
            val n = a.size
            val b = a.copyOf(n + 1)
            b[n] = inner
            if (collectors.compareAndSet(a, b as Array<InnerCollector>)) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun remove(inner: InnerCollector) {
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

            var b = EMPTY as Array<InnerCollector?>
            if (n != 1) {
                b = Array(n - 1) { null }
                a.copyInto(b, 0, 0, j)
                a.copyInto(b, j, j + 1)
            }
            if (collectors.compareAndSet(a, b as Array<InnerCollector>)) {
                return
            }
        }
    }


    private class InnerCollector: Resumable() {
        val consumeReady = Resumable()
    }

    private class Node<T>(val value: T) {
        private val next = atomic<Node<T>?>(null)

        fun get(): Node<T>? = next.value

        fun set(node: Node<T>) {
            next.value = node
        }
    }
}
