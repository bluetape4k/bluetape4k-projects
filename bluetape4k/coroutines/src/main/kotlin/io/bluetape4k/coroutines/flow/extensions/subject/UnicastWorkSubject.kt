package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.exceptions.FlowNoElementException
import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 동시에 하나의 collector만 허용하는 work-queue 형태 Subject입니다.
 *
 * ## 동작/계약
 * - `emit`된 값은 내부 큐에 적재되고, 활성 collector 1개가 순차 소비합니다.
 * - 동일 시점에 두 번째 collector가 진입하면 `IllegalStateException`이 발생합니다.
 * - `complete`/`emitError` 이후에도 큐에 남아 있는 값은 먼저 소비되고, 그 다음 완료 또는 예외가 전파됩니다.
 * - 큐는 `ConcurrentLinkedQueue`를 사용하며 값 누적량에 비례해 메모리를 사용합니다.
 *
 * ```kotlin
 * val subject = UnicastWorkSubject<Int>()
 * repeat(5) { subject.emit(it) }
 * subject.complete()
 * val result = subject.take(3).toList()
 * // result == [0, 1, 2]
 * ```
 */
class UnicastWorkSubject<T: Any>: AbstractFlow<T>(), SubjectApi<T> {

    companion object: KLoggingChannel() {
        @JvmField
        val terminated = FlowNoElementException("No more elements")
    }

    private val resumable = Resumable()
    private val queue = ConcurrentLinkedQueue<T>()

    private val terminal = atomic<Throwable?>(null)
    private val current = atomic<FlowCollector<T>?>(null)

    /**
     * 현재 활성 collector 존재 여부를 반환합니다.
     *
     * ## 동작/계약
     * - 내부 `current`가 `null`이 아니면 `true`를 반환합니다.
     * - 이 Subject는 단일 collector만 허용하므로 `true`인 동안 다른 collect 진입은 실패합니다.
     */
    override val hasCollectors: Boolean
        get() = current.value != null

    /**
     * 현재 collector 수를 반환합니다.
     *
     * ## 동작/계약
     * - 활성 collector가 있으면 1, 없으면 0을 반환합니다.
     * - 다중 collector를 허용하지 않으므로 1보다 큰 값은 반환되지 않습니다.
     */
    override val collectorCount: Int
        get() = if (hasCollectors) 1 else 0

    /**
     * 큐의 값을 순차 소비하며 단일 collector를 등록합니다.
     *
     * ## 동작/계약
     * - 다른 collector가 이미 활성 상태면 `error("Only one collector allowed.")`로 `IllegalStateException`이 발생합니다.
     * - 종료 표식이 있어도 큐에 값이 남아 있으면 해당 값을 먼저 전달합니다.
     * - 종료 표식이 `terminated`이면 정상 반환하고, 다른 예외면 collect 종료 시 해당 예외를 던집니다.
     *
     * ```kotlin
     * val subject = UnicastWorkSubject<Int>()
     * repeat(3) { subject.emit(it) }
 * subject.complete()
     * val result = subject.toList()
     * // result == [0, 1, 2]
     * ```
     *
     * @param collector 큐에서 꺼낸 값을 받을 collector입니다.
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

            if (t != null && v == null) {
                current.getAndSet(null)
                if (t != terminated) {
                    throw t
                }
                return
            }
            if (v != null) {
                try {
                    collector.emit(v)
                } catch (e: Throwable) {
                    current.getAndSet(null)
                    throw e
                }
            } else {
                resumable.await()
            }
        }
    }

    /**
     * 값을 큐에 추가하고 대기 중 collector를 깨웁니다.
     *
     * ## 동작/계약
     * - 값은 즉시 `ConcurrentLinkedQueue`에 적재됩니다.
     * - 활성 collector가 없어도 값은 큐에 남아 이후 collect에서 소비됩니다.
     * - 수신 객체 상태(큐 길이)가 변경됩니다.
     *
     * @param value 큐에 추가할 값입니다.
     */
    override suspend fun emit(value: T) {
        queue.offer(value)
        resumable.resume()
    }

    /**
     * 오류 종료 표식을 설정하고 대기 중 collector를 깨웁니다.
     *
     * ## 동작/계약
     * - `terminal`에 예외를 저장한 뒤 collector 루프를 재개시킵니다.
     * - 큐가 비는 시점에 저장된 예외가 collect 쪽으로 전파됩니다.
     * - `ex`가 `null`이면 종료 표식이 설정되지 않아 즉시 종료로 이어지지 않습니다.
     *
     * @param ex 종료 시 전파할 예외입니다.
     */
    override suspend fun emitError(ex: Throwable?) {
        terminal.value = ex
        resumable.resume()
    }

    /**
     * 정상 종료 표식을 설정하고 대기 중 collector를 깨웁니다.
     *
     * ## 동작/계약
     * - 내부적으로 `terminal = terminated`를 기록합니다.
     * - 큐에 남은 값이 모두 소비된 뒤 collect가 정상 종료됩니다.
     */
    override suspend fun complete() {
        terminal.value = terminated
        resumable.resume()
    }
}
