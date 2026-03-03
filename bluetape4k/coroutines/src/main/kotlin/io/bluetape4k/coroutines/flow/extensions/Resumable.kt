package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 단일 대기자(await)와 단일 재개(resume)를 동기화하는 경량 신호 객체입니다.
 *
 * ## 동작/계약
 * - 동시에 하나의 `await()`만 허용하며, 동시 두 번째 대기는 `IllegalStateException`이 발생합니다.
 * - `resume()`이 먼저 호출되면 다음 `await()`는 즉시 통과합니다.
 * - 내부 상태는 원자 참조로 관리되며 lock을 사용하지 않습니다.
 * - 대기/재개에 필요한 continuation 참조 외 추가 컬렉션 할당이 없습니다.
 *
 * ```kotlin
 * val gate = Resumable()
 * launch { delay(10); gate.resume() }
 * gate.await()
 * // await가 resume 이후 정상 복귀
 * ```
 */
open class Resumable {

    companion object: KLogging() {
        private val READY = ReadyContinuation()

        /**
         * 내부 성공 신호로 사용하는 sentinel 객체입니다.
         */
        val VALUE = Any()

        /**
         * [VALUE] 기반 성공 결과 캐시입니다.
         */
        val RESULT_SUCCESS = Result.success(VALUE)
    }

    private val continuationRef = atomic<Continuation<Any>?>(null)
    private val continuation by continuationRef

    /**
     * 외부에서 [resume] 신호가 올 때까지 현재 코루틴을 대기시킵니다.
     *
     * ## 동작/계약
     * - 이미 재개된 상태면 suspend 없이 즉시 반환합니다.
     * - 다른 코루틴이 이미 대기 중이면 `IllegalStateException`이 발생합니다.
     * - 복귀 직후 내부 continuation 슬롯을 `null`로 되돌립니다.
     */
    suspend fun await() {
        suspendCancellableCoroutine { cont ->
            while (true) {
                val current = continuation
                if (current == READY) {
                    cont.resumeWith(RESULT_SUCCESS)
                    break
                }
                if (current != null) {
                    throw IllegalStateException("Only one thread can await a Resumable")
                }
                if (continuationRef.compareAndSet(current, cont)) {
                    break
                }
            }
        }
        continuationRef.getAndSet(null)
    }

    /**
     * 대기 중인 코루틴을 재개하거나 다음 await를 즉시 통과 상태로 전환합니다.
     *
     * ## 동작/계약
     * - 현재 상태가 이미 재개 완료(`READY`)면 아무 작업 없이 반환합니다.
 * - 대기자가 있으면 성공 결과로 재개시키고, 없으면 재개 완료 상태만 기록합니다.
     * - 여러 번 호출해도 안전하며 최초 재개 이후 호출은 실질적으로 no-op입니다.
     */
    fun resume() {
        if (continuation == READY) {
            return
        }
        continuationRef.getAndSet(READY)?.resumeWith(RESULT_SUCCESS)
    }

    private class ReadyContinuation: Continuation<Any> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any>) {
            // 이 함수가 호출되는 것은 이미 재개를 했다는 의미입니다.
        }
    }
}
