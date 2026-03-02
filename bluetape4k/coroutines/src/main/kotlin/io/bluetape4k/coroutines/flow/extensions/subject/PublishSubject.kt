package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.ResumableCollector
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.CancellationException

/**
 * 구독 시점 이후에 emit된 값만 전달하는 Publish Subject 구현입니다.
 *
 * ## 동작/계약
 * - 과거 값은 재생(replay)하지 않고, 현재 등록된 collector에게만 값을 전달합니다.
 * - `emit`/`complete`/`emitError`는 collector 배열을 원자적으로 교체하며, 수신 객체 내부 상태를 변경합니다.
 * - `emitError`는 최초 1회만 적용되며 이후 신규 collector의 collect 시 저장된 예외가 다시 전파될 수 있습니다.
 * - collector 추가/삭제 시 배열 복사가 발생하므로 collector 변경 비용은 collector 수에 비례합니다.
 *
 * ```kotlin
 * val subject = PublishSubject<Int>()
 * val result = mutableListOf<Int>()
 * coroutineScope {
 *   launch { subject.take(3).toList(result) }
 *   subject.awaitCollector(); subject.emit(1); subject.emit(2); subject.emit(3)
 * }
 * // result == [1, 2, 3]
 * ```
 */
@Suppress("UNCHECKED_CAST")
class PublishSubject<T>: AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val EMPTY = arrayOf<ResumableCollector<Any>>()
        private val TERMINATED = arrayOf<ResumableCollector<Any>>()
    }

    private val collectors: AtomicRef<Array<ResumableCollector<T>>> =
        atomic(EMPTY as Array<ResumableCollector<T>>)

    private var error: Throwable? = null

    /**
     * 현재 등록된 collector가 1개 이상인지 반환합니다.
     *
     * ## 동작/계약
     * - 내부 collector 배열 길이가 0보다 크면 `true`를 반환합니다.
     * - 종료 상태로 전환되면 collector 배열이 `TERMINATED`로 교체되어 `false`가 됩니다.
     */
    override val hasCollectors: Boolean
        get() = collectors.value.isNotEmpty()

    /**
     * 현재 등록된 collector 수를 반환합니다.
     *
     * ## 동작/계약
     * - 반환값은 조회 시점 배열 길이이며 동시성 상황에서 즉시 달라질 수 있습니다.
     * - collector 추가/제거는 CAS 기반 배열 교체로 반영됩니다.
     */
    override val collectorCount: Int
        get() = collectors.value.size

    /**
     * collector를 등록하고 Subject가 종료될 때까지 값을 전달합니다.
     *
     * ## 동작/계약
     * - 종료 전이면 내부 `ResumableCollector`를 추가한 뒤 `drain`으로 값을 전달합니다.
     * - 이미 오류 종료된 상태에서 collect를 시작하면 저장된 `error`를 다시 던집니다.
     * - 정상 종료 상태(`complete`)라면 예외 없이 즉시 반환될 수 있습니다.
     *
     * ```kotlin
     * val subject = PublishSubject<Int>()
     * val result = mutableListOf<Int>()
     * coroutineScope {
     *   launch { subject.take(2).toList(result) }
     *   subject.awaitCollector(); subject.emit(10); subject.emit(20)
     * }
     * // result == [10, 20]
     * ```
     *
     * @param collector 실제 요소를 전달받는 collector입니다.
     */
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        val inner = ResumableCollector<T>()
        if (add(inner)) {
            inner.drain(collector) { this.remove(it) }
            return
        }
        error?.let { throw it }
    }

    /**
     * 현재 등록된 모든 collector에게 값을 전파합니다.
     *
     * ## 동작/계약
     * - 호출 시점 collector 스냅샷에 대해 순회하며 `next(value)`를 호출합니다.
     * - 전달 중 `CancellationException`이 발생한 collector는 목록에서 제거합니다.
     * - 구독자가 없으면 아무 동작 없이 반환하며 값을 버퍼링하지 않습니다.
     *
     * ```kotlin
     * val subject = PublishSubject<Int>()
     * val result = mutableListOf<Int>()
     * coroutineScope {
     *   launch { subject.take(1).toList(result) }
     *   subject.awaitCollector(); subject.emit(7)
     * }
     * // result == [7]
     * ```
     *
     * @param value 전파할 값입니다.
     */
    override suspend fun emit(value: T) {
        collectors.value.forEach { collector ->
            try {
                collector.next(value)
            } catch (e: CancellationException) {
                remove(collector)
            }
        }
    }

    /**
     * Subject를 오류 종료하고 현재 collector들에게 오류를 전파합니다.
     *
     * ## 동작/계약
     * - 최초 1회 호출에서만 `error`를 저장하고 collector 배열을 종료 상태로 교체합니다.
     * - 이미 종료된 이후 재호출하면 추가 동작 없이 반환합니다.
     * - 이후 신규 collect는 저장된 오류를 다시 전파할 수 있습니다.
     *
     * ```kotlin
     * val subject = PublishSubject<Int>()
     * subject.emitError(IllegalStateException("boom"))
     * // 이후 collect 시 IllegalStateException 전파 가능
     * ```
     *
     * @param ex 전파할 종료 원인 예외입니다.
     */
    override suspend fun emitError(ex: Throwable?) {
        if (this.error == null) {
            this.error = ex
            val colls = collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
            colls.forEach { collector ->
                runCatching { collector.error(ex) }
            }
        }
    }

    /**
     * Subject를 정상 종료하고 현재 collector에게 완료 신호를 보냅니다.
     *
     * ## 동작/계약
     * - collector 배열을 종료 상태로 교체한 뒤 각 collector에 `complete()`를 호출합니다.
     * - 완료 이후 신규 collect는 즉시 반환될 수 있으며 과거 값은 재생되지 않습니다.
     * - 종료 전 등록된 collector가 없으면 상태 전환만 수행하고 반환합니다.
     */
    override suspend fun complete() {
        val colls = collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
        colls.forEach { collector ->
            runCatching { collector.complete() }
        }
    }

    private fun add(inner: ResumableCollector<T>): Boolean {
        while (true) {
            val a = collectors.value
            if (areEqualAsAny(a, TERMINATED)) {
                return false
            }
            val n = a.size
            val b = a.copyOf(n + 1)
            b[n] = inner
            if (collectors.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return true
            }
        }
    }

    private fun remove(inner: ResumableCollector<T>) {
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

            var b = EMPTY as Array<ResumableCollector<T>?>
            if (n != 1) {
                b = Array(n - 1) { null }
                a.copyInto(b, 0, 0, j)
                a.copyInto(b, j, j + 1)
            }
            if (collectors.compareAndSet(a, b as Array<ResumableCollector<T>>)) {
                return
            }
        }
    }
}
