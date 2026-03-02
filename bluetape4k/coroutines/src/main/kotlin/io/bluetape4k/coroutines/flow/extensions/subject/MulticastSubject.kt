package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.coroutines.flow.extensions.ResumableCollector
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector

/**
 * 지정한 collector 수가 준비될 때까지 producer를 대기시키는 multicast Subject입니다.
 *
 * ## 동작/계약
 * - 생성 시 지정한 collector 수(`expectedCollectorSize`)가 모두 등록되기 전까지 `emit`은 suspend 대기합니다.
 * - 기준 수를 한 번 충족한 이후에는 이후 `emit` 호출에서 추가 대기를 하지 않습니다.
 * - `emit`된 값은 현재 등록된 collector 전원에게 전달되며, 취소된 collector는 제거됩니다.
 * - collector 추가/제거는 배열 복사 기반(CAS)으로 처리되어 collector 변동 비용은 collector 수에 비례합니다.
 *
 * ```kotlin
 * val subject = MulticastSubject<Int>(2)
 * // 두 collector가 등록되기 전 emit은 대기한다.
 * // 등록 완료 후 emit(1) 호출 시 두 collector가 모두 1을 받는다.
 * ```
 */
class MulticastSubject<T> private constructor(
    expectedCollectorSize: Int,
): AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val EMPTY = arrayOf<ResumableCollector<Any>>()
        private val TERMINATED = arrayOf<ResumableCollector<Any>>()
        private val DONE = FlowOperationException("Subject completed")

        /**
         * MulticastSubject 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `expectedCollectorSize`가 1 미만이면 1로 보정합니다.
         * - 보정된 수만큼 초기 구독자가 모일 때까지 첫 `emit`들이 대기할 수 있습니다.
         *
         * @param expectedCollectorSize producer 진행 전에 대기할 최소 collector 수입니다.
         */
        operator fun <T> invoke(expectedCollectorSize: Int): MulticastSubject<T> {
            return MulticastSubject(expectedCollectorSize.coerceAtLeast(1))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val collectors = atomic(EMPTY as Array<ResumableCollector<T>>)

    private val producer = Resumable()

    private val remainingCollectors = atomic(expectedCollectorSize)

    @Volatile
    private var terminated: Throwable? = null

    /**
     * 현재 등록된 collector가 1개 이상인지 반환합니다.
     *
     * ## 동작/계약
     * - 내부 collector 배열이 비어 있지 않으면 `true`를 반환합니다.
     * - 종료 상태에서는 배열이 `TERMINATED`로 교체되어 `false`가 됩니다.
     */
    override val hasCollectors: Boolean
        get() = collectors.value.isNotEmpty()

    /**
     * 현재 등록된 collector 수를 반환합니다.
     *
     * ## 동작/계약
     * - 반환값은 조회 시점 배열 길이입니다.
     * - collector 취소/해제와 동시 실행 시 값이 즉시 달라질 수 있습니다.
     */
    override val collectorCount: Int
        get() = collectors.value.size

    /**
     * 현재 등록된 collector들에게 값을 multicast 전송합니다.
     *
     * ## 동작/계약
     * - `remainingCollectors > 0`이면 최소 구독자 수가 채워질 때까지 suspend 대기합니다.
     * - 대기 조건이 풀리면 호출 시점 collector 전원에게 `next(value)`를 호출합니다.
     * - 전달 중 `CancellationException`이 난 collector는 목록에서 제거됩니다.
     *
     * @param value 전송할 값입니다.
     */
    override suspend fun emit(value: T) {
        awaitCollectors()
        collectors.value.forEach { collector ->
            try {
                collector.next(value)
            } catch (e: CancellationException) {
                remove(collector)
            }
        }
    }

    /**
     * Subject를 오류 종료하고 현재 collector에게 예외를 전달합니다.
     *
     * ## 동작/계약
     * - `terminated`에 예외를 저장한 뒤 collector 배열을 종료 상태로 교체합니다.
     * - 교체 시점의 collector 각각에 `error(ex)`를 호출합니다.
     * - 종료 이후 신규 collect는 등록에 실패하고 저장된 예외가 재전파될 수 있습니다.
     *
     * @param ex 종료 원인 예외입니다.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun emitError(ex: Throwable?) {
        terminated = ex
        collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
            .forEach { collector ->
                try {
                    collector.error(ex)
                } catch (_: CancellationException) {
                    // ignored at this point
                }
            }
    }

    /**
     * Subject를 정상 종료하고 현재 collector에게 완료를 전달합니다.
     *
     * ## 동작/계약
     * - 내부 종료 원인을 `DONE`으로 기록하고 collector 배열을 종료 상태로 교체합니다.
     * - 교체 시점의 collector 각각에 `complete()`를 호출합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun complete() {
        terminated = DONE
        collectors.getAndSet(TERMINATED as Array<ResumableCollector<T>>)
            .forEach { collector ->
                try {
                    collector.complete()
                } catch (_: CancellationException) {
                    log.debug { "$collector is cancelled." }
                }
            }
    }

    /**
     * collector를 등록하고 종료 시점까지 값을 전달합니다.
     *
     * ## 동작/계약
     * - 종료 전이면 내부 collector를 등록하고, 등록 순서에 따라 `remainingCollectors`를 감소시킵니다.
     * - `remainingCollectors`가 0이 되는 순간 producer를 재개시켜 대기 중 `emit`을 진행시킵니다.
     * - 이미 종료 상태면 등록 없이 즉시 반환하며, 오류 종료였으면 저장된 예외를 던집니다.
     *
     * ```kotlin
     * val subject = MulticastSubject<Int>(1)
     * val result = mutableListOf<Int>()
     * coroutineScope {
     *   launch { subject.take(2).toList(result) }
     *   subject.awaitCollector(); subject.emit(1); subject.emit(2); subject.complete()
     * }
     * // result == [1, 2]
     * ```
     *
     * @param collector 전달받은 값을 소비할 collector입니다.
     */
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
            val array = collectors.value
            if (areEqualAsAny(array, TERMINATED)) {
                return false
            }
            val n = array.size
            val b = array.copyOf(n + 1)
            b[n] = inner
            if (collectors.compareAndSet(array, b as Array<ResumableCollector<T>>)) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun remove(inner: ResumableCollector<T>) {
        while (true) {
            val array = collectors.value
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
            if (collectors.compareAndSet(array, b as Array<ResumableCollector<T>>)) {
                return
            }
        }
    }
}
