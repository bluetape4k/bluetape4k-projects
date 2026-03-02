package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive

/**
 * 가장 최근 값을 유지하고 새 collector에게 즉시 전달하는 Subject입니다.
 *
 * ## 동작/계약
 * - 새 collector는 구독 즉시 최신 값(있다면)을 먼저 받고 이후 실시간 값을 받습니다.
 * - 기본 생성 시 초기값이 없고, `invoke(initialValue)`로 초기값을 줄 수 있습니다.
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
        @Suppress("UNCHECKED_CAST")
        operator fun <T: Any> invoke(initialValue: T = NONE as T): BehaviorSubject<T> {
            return BehaviorSubject(Node(initialValue))
        }
    }

    private val collectors = atomic<Array<InnerCollector>>(EMPTY)

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
     * - 최신 노드를 교체한 뒤 collector들을 깨워 새 값을 전달합니다.
     * - collector가 취소되면 내부 목록에서 제거합니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.emit(2)
     * // 최신값은 2
     * ```
     * @param value 방출할 값입니다.
     */
    override suspend fun emit(value: T) {
        if (current == DONE)
            return

        val next = Node(value)
        current.set(next)
        current = next

        collectors.value.forEach { innerCollector ->
            try {
                innerCollector.consumeReady.await()
                innerCollector.resume()
            } catch (e: CancellationException) {
                remove(innerCollector)
            }
        }
    }

    /**
     * 오류 종료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 최초 종료 전환 시 error를 저장하고 `DONE` 노드로 마킹합니다.
     * - 활성 collector를 모두 깨워 종료 경로로 진행시킵니다.
     * - 이후 emit/complete/emitError 호출은 무시됩니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.emitError(RuntimeException("boom"))
     * // collector는 1 이후 예외를 받음
     * ```
     * @param ex 종료 원인 예외입니다.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun emitError(ex: Throwable?) {
        if (current == DONE)
            return

        error = ex
        current.set(DONE as Node<T>)
        current = DONE

        collectors.getAndSet(TERMINATED).forEach { innerCollector ->
            runCatching {
                innerCollector.consumeReady.await()
                innerCollector.resume()
            }
        }
    }

    /**
     * 정상 완료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 최초 종료 전환 시 `DONE` 노드로 마킹하고 collector를 모두 깨웁니다.
     * - collector는 남은 값을 처리한 뒤 정상 종료합니다.
     * - 이후 emit/complete/emitError 호출은 무시됩니다.
     *
     * ```kotlin
     * subject.emit(1)
     * subject.complete()
     * // collector는 1 이후 정상 완료
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun complete() {
        if (current == DONE)
            return

        current.set(DONE as Node<T>)
        current = DONE

        collectors.getAndSet(TERMINATED).forEach { innerCollector ->
            runCatching {
                innerCollector.consumeReady.await()
                innerCollector.resume()
            }.onFailure {
                log.error(it) { "Fail to complete. innerCollector=$innerCollector" }
            }
        }
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
