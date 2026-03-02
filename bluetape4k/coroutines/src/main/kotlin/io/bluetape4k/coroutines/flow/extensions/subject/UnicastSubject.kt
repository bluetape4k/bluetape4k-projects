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
 * 단일 collector만 허용하는 Subject입니다.
 *
 * ## 동작/계약
 * - collector는 최대 1개만 등록할 수 있고, 두 번째 collect 시 `IllegalStateException`이 발생합니다.
 * - collector가 붙기 전 emit 값은 내부 큐에 버퍼링되며, collector가 시작되면 순서대로 전달됩니다.
 * - `complete()` 또는 `emitError()` 이후 종료 상태가 되며, 이후 재수집은 허용되지 않습니다.
 * - 내부 큐는 `ConcurrentLinkedQueue`를 사용하며 emit 수에 비례해 메모리 사용량이 증가할 수 있습니다.
 *
 * ```kotlin
 * val us = UnicastSubject<Int>()
 * us.emit(1); us.emit(2); us.complete()
 * val out = us.toFastList()
 * // out == [1, 2]
 * ```
 */
class UnicastSubject<T: Any>: AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        private val terminatedCollector = FlowCollector<Any?> {
            log.trace { "TerminatedCollector was called." }
        }
        private val terminated = FlowNoElementException("No more elements")
    }

    /**
     * producer/consumer 동기화에 사용하는 수동 resume 신호기입니다.
     *
     * ## 동작/계약
     * - emit/complete/error 시 `resume()`되어 대기 중 collector를 깨웁니다.
     * - Subject 외부에서 직접 제어할 수 있으므로, 일반 사용자는 read-only 용도로만 접근하는 것을 권장합니다.
     * - 수신 객체 상태를 바꾸는 동기화 primitive입니다.
     *
     * ```kotlin
     * val signal = us.resumable
     * // signal != null
     * ```
     */
    val resumable = Resumable()

    private val queue = ConcurrentLinkedQueue<T>()
    private val terminal = atomic<Throwable?>(null)

    private val current = atomic<FlowCollector<T>?>(null)

    /**
     * collector가 종료 상태로 전환되었는지 반환합니다.
     *
     * ## 동작/계약
     * - 내부 current collector가 종료 sentinel이면 `true`를 반환합니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val cancelled = us.collectorCancelled
     * // cancelled == false || true
     * ```
     */
    val collectorCancelled: Boolean
        get() = current.value == terminatedCollector

    /**
     * 현재 활성 collector 존재 여부를 반환합니다.
     *
     * ## 동작/계약
     * - collector가 등록되어 있고 종료 sentinel이 아니면 `true`입니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val has = us.hasCollectors
     * // has == false || true
     * ```
     */
    override val hasCollectors: Boolean
        get() = current.value != null && current.value != terminatedCollector

    /**
     * 현재 활성 collector 수를 반환합니다.
     *
     * ## 동작/계약
     * - Unicast 특성상 결과는 `0` 또는 `1`입니다.
     * - 조회 전용이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val count = us.collectorCount
     * // count == 0 || count == 1
     * ```
     */
    override val collectorCount: Int
        get() = if (hasCollectors) 1 else 0

    /**
     * 단일 collector를 등록하고 버퍼/실시간 값을 소비합니다.
     *
     * ## 동작/계약
     * - 이미 collector가 있으면 즉시 `IllegalStateException("Only one collector allowed.")`를 던집니다.
     * - 큐에 쌓인 값을 먼저 emit하고, 값이 없으면 [resumable] 신호를 기다립니다.
     * - terminal이 오류면 큐를 모두 보낸 뒤 해당 오류를 던지고 종료합니다.
     * - collector emit 중 예외가 나면 종료 sentinel로 전환하고 큐를 비운 뒤 예외를 전파합니다.
     *
     * ```kotlin
     * val us = UnicastSubject<Int>()
     * us.emit(1); us.complete()
     * val out = us.toFastList()
     * // out == [1]
     * ```
     * @param collector 값을 수집할 collector입니다.
     */
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
            val t = terminal.value
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

    /**
     * 값을 큐에 적재하고 collector를 깨웁니다.
     *
     * ## 동작/계약
     * - 종료 sentinel 상태가 아니면 값을 큐에 넣고 `resume()`을 호출합니다.
     * - 종료 sentinel 상태면 새 값을 버리고 큐를 정리합니다.
     * - 수신 객체 내부 큐 상태를 변경(mutate)합니다.
     *
     * ```kotlin
     * us.emit(1)
     * us.emit(2)
     * // collector가 있으면 순서대로 전달
     * ```
     * @param value 방출할 값입니다.
     */
    override suspend fun emit(value: T) {
        if (current.value != terminatedCollector) {
            queue.offer(value)
            resumable.resume()
        } else {
            queue.clear()
        }
    }

    /**
     * 오류 종료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - terminal에 [ex]를 저장하고 대기 중 collector를 깨웁니다.
     * - collector는 남은 큐를 소비한 뒤 [ex]를 받습니다.
     * - 이후 재수집은 허용되지 않습니다.
     *
     * ```kotlin
     * us.emit(1)
     * us.emitError(RuntimeException("boom"))
     * // collect 시 1 처리 후 예외 전파
     * ```
     * @param ex 종료 원인 예외입니다.
     */
    override suspend fun emitError(ex: Throwable?) {
        terminal.value = ex
        resumable.resume()
    }

    /**
     * 정상 완료 상태로 전환합니다.
     *
     * ## 동작/계약
     * - terminal을 내부 완료 sentinel로 설정하고 대기 중 collector를 깨웁니다.
     * - collector는 남은 큐를 소비한 뒤 정상 종료합니다.
     * - 이후 재수집은 허용되지 않습니다.
     *
     * ```kotlin
     * us.emit(1)
     * us.complete()
     * // collect 결과 == [1]
     * ```
     */
    override suspend fun complete() {
        terminal.value = terminated
        resumable.resume()
    }
}
