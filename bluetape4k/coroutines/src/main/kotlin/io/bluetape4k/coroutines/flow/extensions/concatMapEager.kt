package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.experimental.ExperimentalTypeInference

private val log: Logger by lazy { KotlinLogging.logger { } }

/**
 * inner Flow를 eager하게 동시 수집하되 결과는 source 순서대로 연결(concat)해 방출합니다.
 *
 * ## 동작/계약
 * - source 요소마다 `transform`을 즉시 실행해 inner 수집 코루틴을 시작합니다.
 * - inner 수집은 동시 실행되지만 출력은 source 순서의 큐를 순차 비우며 방출합니다.
 * - 각 inner는 자체 `ConcurrentLinkedQueue`를 사용해 값을 버퍼링합니다.
 * - source/inner 예외 처리 규칙은 `channelFlow`와 코루틴 취소 규칙을 따릅니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).concatMapEager { v -> flowOf(v, v * 10) }.toList()
 * // out == [1, 10, 2, 20]
 * ```
 *
 * @param transform source 값을 inner Flow로 변환하는 함수입니다.
 */
fun <T: Any, R: Any> Flow<T>.concatMapEager(transform: suspend (T) -> Flow<R>): Flow<R> =
    concatMapEagerInternal(transform)

@OptIn(ExperimentalTypeInference::class)
internal fun <T: Any, R: Any> Flow<T>.concatMapEagerInternal(
    transform: suspend (T) -> Flow<R>,
): Flow<R> = channelFlow {
    coroutineScope {
        val resumeOutput = Resumable()
        val innerQueues = ConcurrentLinkedQueue<ConcatMapEagerInnerQueue<R>>()
        val state = ConcatMapEagerState()

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
                state.innerDone.value = true
                resumeOutput.resume()
            }
        }

        var innerQueue: ConcatMapEagerInnerQueue<R>? = null
        while (isActive) {
            if (innerQueue == null) {
                val done = state.innerDone.value
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
            resumeOutput.await()
        }
    }
}

private class ConcatMapEagerInnerQueue<R: Any> {
    val queue = ConcurrentLinkedQueue<R>()
    val done = atomic(false)
}

private class ConcatMapEagerState {
    val innerDone = atomic(false)
}
