package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.unsafeLazy
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.ExperimentalTypeInference

private val log: Logger by unsafeLazy { KotlinLogging.logger { } }

/**
 * 업스트림 값을 [Flow]로 매핑하고 한번에 모두 시작한 다음, 다른 소스의 항목이 발행되기 전에 첫 번째 소스에서 모든 항목을 발행합니다.
 * 각 소스는 무제한으로 소비되므로 현재 소스와 수집기의 속도에 따라 연산자가 항목을 더 오래 유지하고 실행 중에 더 많은 메모리를 사용할 수 있습니다.
 *
 * ```
 * flowRangeOf(1, 5)              // 1,2,3,4,5
 *     .concatMapEager {
 *         flowRangeOf(it * 10, 5).delay(100)   // 10,11,12,13,14
 *     }                                        // 20,21,22,23,24 ...
 *     .take(7)
 *     .assertResult(
 *         10, 11, 12, 13, 14,
 *         20, 21
 *     )
 * ```
 *
 * @param transform 업스트림 요소를 [Flow]로 변환하는 suspendable 함수
 *
 * @return a [Flow] that emits items from the sources
 */
fun <T: Any, R: Any> Flow<T>.concatMapEager(transform: suspend (T) -> Flow<R>): Flow<R> =
    concatMapEagerInternal(transform)

/**
 * 업스트림 값을 [Flow]로 매핑하고 한번에 모두 시작한 다음, 다른 소스의 항목이 발행되기 전에 첫 번째 소스에서 모든 항목을 발행합니다.
 * 각 소스는 무제한으로 소비되므로 현재 소스와 수집기의 속도에 따라 연산자가 항목을 더 오래 유지하고 실행 중에 더 많은 메모리를 사용할 수 있습니다.
 *
 * @param T 소스 요소 타입
 * @param R 변환된 요소 타입
 * @param transform 업스트림 요소를 [Flow]로 변환하는 suspendable 함수
 * @receiver source flow
 * @return 변환된 요소를 발행하는 Flow
 */
@OptIn(ExperimentalTypeInference::class)
internal fun <T: Any, R: Any> Flow<T>.concatMapEagerInternal(
    @BuilderInference transform: suspend (T) -> Flow<R>,
): Flow<R> = channelFlow {
    coroutineScope {
        val resumeOutput = Resumable()
        val innerQueues = ConcurrentLinkedQueue<ConcatMapEagerInnerQueue<R>>()
        val innerDone = AtomicBoolean(false)

        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                collect { item ->
                    log.trace { "source item=$item" }
                    val f = transform(item)
                    val newQueue = ConcatMapEagerInnerQueue<R>()
                    innerQueues.offer(newQueue)
                    resumeOutput.resume()
                    launch {
                        try {
                            f.collect {
                                log.trace { "mapped item=$it" }
                                newQueue.queue.offer(it)
                                resumeOutput.resume()
                            }
                        } finally {
                            newQueue.done.value = true
                            resumeOutput.resume()
                        }
                    }
                }
            } finally {
                innerDone.set(true)
                resumeOutput.resume()
            }
        }

        var innerQueue: ConcatMapEagerInnerQueue<R>? = null
        while (isActive) {
            if (innerQueue == null) {
                val done = innerDone.get()
                innerQueue = innerQueues.poll()

                if (done && innerQueue == null) {
                    break
                }
            }
            if (innerQueue != null) {
                val done = innerQueue.done.value
                val value = innerQueue.queue.poll()

                if (done && value == null) {
                    innerQueue = null
                    continue
                }
                if (value != null) {
                    send(value)
                    continue
                }
            }
            // 다음 item이 올때까지 대기한다
            resumeOutput.await()
        }
    }
}

private class ConcatMapEagerInnerQueue<R: Any> {
    val queue = ConcurrentLinkedQueue<R>()
    val done = atomic(false)
}
