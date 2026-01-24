package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.StopFlowException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 상태 정보(값, 예외, 완료 여부)를 가지고, 상태변화를 보관하고 있다가,
 * Consumer가 준비되면(drain 함수 호출)으로 값을 전달하는 Queue 이다
 */
class ResumableCollector<T>: Resumable() {

    companion object: KLogging()

    @Volatile
    var value: T = uninitialized()
    var error: Throwable? = null

    private val done = AtomicBoolean(false)
    private val hasValue = AtomicBoolean(false)

    private val consumerReady = Resumable()

    suspend fun next(value: T) {
        whenConsumerReady {
            this.value = value
            this.hasValue.set(true)
        }
    }

    suspend fun error(error: Throwable?) {
        whenConsumerReady {
            this.error = error
            this.done.set(true)
        }
    }

    suspend fun complete() {
        whenConsumerReady {
            this.done.set(true)
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

    fun readyConsumer() {
        consumerReady.resume()
    }

    /**
     * 현 Coroutine이 Active인 동안에는 값을 [collector]에 전달해서 버퍼링하고,
     * 완료나 예외가 발생하면, [onComplete] 를 수행한다.
     *
     * @param collector 버퍼링할 [FlowCollector]
     * @param onComplete 완료나 예외가 발생하면 수행할 함수
     */
    suspend fun drain(collector: FlowCollector<T>, onComplete: ((ResumableCollector<T>) -> Unit)? = null) =
        coroutineScope {
            while (coroutineContext.isActive) {
                readyConsumer()
                awaitSignal()

                if (hasValue.get()) {
                    val v = value
                    value = uninitialized()
                    hasValue.set(false)

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

                if (done.get()) {
                    error?.let { throw it }
                    break
                }
            }
        }
}
