package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector

/**
 * 단일 생산자/단일 소비자 버퍼를 이용해 값을 비동기 전달하는 재개 가능한 collector입니다.
 *
 * ## 동작/계약
 * - 내부적으로 `SpscArrayQueue`를 사용해 값을 버퍼링하고, 소비 속도가 느리면 생산자가 suspend 대기합니다.
 * - `drain` 중 collector 예외가 발생하면 `cancelled`를 설정하고 이후 `next`는 `CancellationException`으로 종료됩니다.
 * - `error` 또는 `complete` 호출 후 버퍼가 비면 drain 루프가 종료되며, `error`가 있으면 해당 예외를 전파합니다.
 * - 용량(`capacity`) 기반 고정 버퍼를 사용하며 추가 컬렉션 할당 없이 슬롯 재사용 중심으로 동작합니다.
 *
 * ```kotlin
 * val rc = BufferedResumableCollector<Int>(4)
 * rc.next(1); rc.next(2); rc.complete()
 * val result = mutableListOf<Int>()
 * rc.drain(collector = FlowCollector { result += it })
 * // result == [1, 2]
 * ```
 */
class BufferedResumableCollector<T> private constructor(capacity: Int): Resumable() {

    companion object: KLoggingChannel() {
        /**
         * 지정한 버퍼 크기로 [BufferedResumableCollector]를 생성합니다.
         *
         * ## 동작/계약
         * - `capacity`가 1 미만이면 1로 보정합니다.
         * - 보정된 크기는 내부 `SpscArrayQueue` 용량으로 사용됩니다.
         *
         * @param capacity 버퍼 슬롯 수입니다. 1 미만이면 1로 보정됩니다.
         */
        @JvmStatic
        operator fun <T> invoke(capacity: Int): BufferedResumableCollector<T> {
            return BufferedResumableCollector(capacity.coerceAtLeast(1))
        }
    }

    private val queue: SpscArrayQueue<T> = SpscArrayQueue(capacity)

    private val done = atomic(false)
    private val cancelled = atomic(false)

    @Volatile
    private var error: Throwable? = null

    private val available = atomic(0L)

    private val valueReady = Resumable()

    private val output: Array<Any?> = Array(1) { null }
    private val limit: Int = capacity - (capacity shr 2)

    /**
     * 값을 버퍼에 추가합니다.
     *
     * ## 동작/계약
     * - 버퍼가 가득 찬 동안 소비자가 비울 때까지 suspend 대기합니다.
     * - 성공적으로 적재되면 대기 중 소비자를 깨웁니다.
     * - drain 측 취소가 이미 반영된 경우 `CancellationException`을 던집니다.
     *
     * @param value 버퍼에 추가할 값입니다.
     */
    suspend fun next(value: T) {
        while (!cancelled.value) {
            if (queue.offer(value)) {
                if (available.getAndIncrement() == 0L) {
                    valueReady.resume()
                }
                break
            }
            await()
        }
        if (cancelled.value) {
            throw CancellationException("Cancel in next.")
        }
    }

    /**
     * 오류 종료를 기록하고 drain 루프를 깨웁니다.
     *
     * ## 동작/계약
     * - 전달받은 예외를 저장하고 `done = true`로 전환합니다.
     * - 버퍼가 모두 비워진 뒤 `drain`에서 저장된 예외가 전파됩니다.
     *
     * @param ex 종료 시 전파할 예외입니다.
     */
    fun error(ex: Throwable?) {
        error = ex
        done.value = true
        valueReady.resume()
    }

    /**
     * 정상 종료를 기록하고 drain 루프를 깨웁니다.
     *
     * ## 동작/계약
     * - `done = true`로 전환합니다.
     * - 버퍼에 남은 값을 모두 전달한 뒤 `drain`이 정상 종료됩니다.
     */
    fun complete() {
        done.value = true
        valueReady.resume()
    }

    /**
     * 버퍼의 값을 collector로 비우고 완료 또는 오류까지 처리합니다.
     *
     * ## 동작/계약
     * - 버퍼에 값이 있으면 즉시 `collector.emit`을 호출하고, 없으면 `valueReady.await()`로 대기합니다.
     * - `collector.emit`이 예외를 던지면 `onCrash`를 호출하고 취소 상태로 전환한 뒤 예외를 다시 던집니다.
     * - `done == true`이고 버퍼가 비었을 때 종료하며, 저장된 `error`가 있으면 종료 시점에 전파합니다.
     *
     * ```kotlin
     * val rc = BufferedResumableCollector<Int>(2)
     * rc.next(10); rc.complete()
     * val result = mutableListOf<Int>()
     * rc.drain(collector = FlowCollector { result += it })
     * // result == [10]
     * ```
     *
     * @param collector 버퍼에서 꺼낸 값을 소비할 collector입니다.
     * @param onCrash `collector.emit` 실패 시 호출할 콜백입니다.
     */
    suspend fun drain(
        collector: FlowCollector<T>,
        onCrash: ((BufferedResumableCollector<T>) -> Unit)? = null,
    ) {
        var consumed = 0L
        val limit = this.limit.toLong()

        while (true) {
            val ne = !queue.poll(output)

            if (done.value && ne) {
                error?.let { throw it }
                break
            }

            if (!ne) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    collector.emit(output[0] as T)
                } catch (ex: Throwable) {
                    onCrash?.invoke(this)
                    cancelled.value = true
                    resume()

                    throw ex
                }

                if (consumed++ == limit) {
                    available.addAndGet(-consumed)
                    consumed = 0L
                    resume()
                }

                continue
            }

            if (available.addAndGet(-consumed) == 0L) {
                resume()
                valueReady.await()
            }
            consumed = 0L
        }
    }
}
