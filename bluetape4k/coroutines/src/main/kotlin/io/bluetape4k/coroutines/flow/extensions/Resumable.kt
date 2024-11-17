package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 요청 시 코루틴을 일시 중단하고 다시 시작할 수 있게 하는 기본 요소입니다.
 *
 * ```
 * val resumable = Resumable()
 * delay(1000)
 * resumable.resume()
 * ```
 */
open class Resumable {

    companion object: KLogging() {
        private val READLY = ReadyContinuation()
        val VALUE = Any()

        val RESULT_SUCCESS = Result.success(VALUE)
    }

    private val continuationRef = atomic<Continuation<Any>?>(null)
    private val continuation by continuationRef

    suspend fun await() {
        suspendCancellableCoroutine { cont ->
            while (true) {
                val current = continuation
                if (current == READLY) {
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

    fun resume() {
        if (continuation == READLY) {
            return
        }
        continuationRef.getAndSet(READLY)?.resumeWith(RESULT_SUCCESS)
    }

    /**
     * continuation이 이미 재개가 준비된 상태라면 suspend (유예) 시킬 필요가 없다는 것을 나타내는 상태 없는 표시자입니다.
     */
    private class ReadyContinuation: Continuation<Any> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Any>) {
            // 이 함수가 호출되는 것은 이미 재개를 했다는 의미입니다.
        }
    }
}
