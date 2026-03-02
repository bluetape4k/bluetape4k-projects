package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.StopFlowException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive

/**
 * producer와 consumer 사이를 수동 핸드셰이크로 연결하는 단일 슬롯 collector입니다.
 *
 * ## 동작/계약
 * - `next/error/complete`는 consumer 준비 신호 이후에만 상태를 갱신합니다.
 * - `drain`은 값 1건 소비 후 producer를 다시 깨우는 핸드셰이크 루프를 수행합니다.
 * - 완료 시 error가 있으면 예외를 던지고, 없으면 정상 종료합니다.
 * - 단일 슬롯(`value`) 구조라 동시 다중 producer에는 적합하지 않습니다.
 *
 * ```kotlin
 * val rc = ResumableCollector<Int>()
 * rc.readyConsumer()
 * // producer는 rc.next(value) 호출 가능
 * ```
 */
class ResumableCollector<T>: Resumable() {

    companion object: KLogging()

    @Volatile
    var value: T = uninitialized()

    var error: Throwable? = null

    private val done = atomic(false)
    private val hasValue = atomic(false)

    private val consumerReady = Resumable()

    /**
     * 다음 값을 단일 슬롯에 적재합니다.
     *
     * ## 동작/계약
     * - consumer가 준비될 때까지 대기한 뒤 값을 저장하고 신호를 보냅니다.
     * - 기존 슬롯 값은 새 값으로 덮어씁니다.
     * - 수신 객체 내부 상태(value/hasValue)를 변경합니다.
     *
     * ```kotlin
     * rc.next(1)
     * // 이후 drain 측에서 1 소비
     * ```
     * @param value 전달할 값입니다.
     */
    suspend fun next(value: T) {
        whenConsumerReady {
            this.value = value
            this.hasValue.value = true
        }
    }

    /**
     * 오류 종료 상태를 설정합니다.
     *
     * ## 동작/계약
     * - consumer가 준비될 때까지 대기한 뒤 종료 상태와 오류를 기록합니다.
     * - `drain`은 이후 루프에서 해당 오류를 던집니다.
     * - 수신 객체 내부 상태(error/done)를 변경합니다.
     *
     * ```kotlin
     * rc.error(RuntimeException("boom"))
     * // drain은 boom 예외로 종료
     * ```
     * @param error 종료 원인 예외입니다.
     */
    suspend fun error(error: Throwable?) {
        whenConsumerReady {
            this.error = error
            this.done.value = true
        }
    }

    /**
     * 정상 완료 상태를 설정합니다.
     *
     * ## 동작/계약
     * - consumer가 준비될 때까지 대기한 뒤 완료 플래그를 설정합니다.
 * - `drain`은 남은 값을 처리한 뒤 정상 종료합니다.
     * - 수신 객체 내부 상태(done)를 변경합니다.
     *
     * ```kotlin
     * rc.complete()
     * // drain은 정상 종료
     * ```
     */
    suspend fun complete() {
        whenConsumerReady {
            this.done.value = true
        }
    }

    private suspend inline fun whenConsumerReady(action: () -> Unit) {
        consumerReady.await()
        action()
        resume()
    }

    private suspend fun awaitSignal() {
        await()
    }

    /**
     * consumer가 값을 받을 준비가 되었음을 알립니다.
     *
     * ## 동작/계약
     * - producer(`next/error/complete`) 대기 지점을 깨웁니다.
     * - 상태값을 변경하지 않고 동기화 신호만 전달합니다.
     *
     * ```kotlin
     * rc.readyConsumer()
     * // producer 진행 가능
     * ```
     */
    fun readyConsumer() {
        consumerReady.resume()
    }

    /**
     * 내부 슬롯 값을 [collector]로 전달하는 소비 루프를 실행합니다.
     *
     * ## 동작/계약
     * - 루프마다 producer 준비 신호를 보낸 뒤 새로운 신호를 기다립니다.
     * - 값이 있으면 emit 후 슬롯을 비웁니다.
     * - emit 중 예외가 나면 [onComplete]를 호출하고 예외를 전파합니다.
     * - done 상태에서 error가 있으면 예외를 던지고, 없으면 정상 종료합니다.
     *
     * ```kotlin
     * rc.drain(FlowCollector { v -> println(v) })
     * // next로 들어온 값들을 순차 소비
     * ```
     * @param collector 값을 소비할 collector입니다.
     * @param onComplete 종료/예외 시 후처리 콜백입니다.
     */
    suspend fun drain(collector: FlowCollector<T>, onComplete: ((ResumableCollector<T>) -> Unit)? = null) =
        coroutineScope {
            while (coroutineContext.isActive) {
                readyConsumer()
                awaitSignal()

                if (hasValue.value) {
                    val v = value
                    value = uninitialized()
                    hasValue.value = false

                    try {
                        if (coroutineContext.isActive) {
                            collector.emit(v)
                            // log.trace { "drain value. v=$v" }
                        } else {
                            throw StopFlowException("current coroutine is not active")
                        }
                    } catch (ex: Throwable) {
                        onComplete?.invoke(this@ResumableCollector)
                        readyConsumer()             // unblock waiters
                        throw ex
                    }
                }

                if (done.value) {
                    error?.let { throw it }
                    break
                }
            }
        }
}
