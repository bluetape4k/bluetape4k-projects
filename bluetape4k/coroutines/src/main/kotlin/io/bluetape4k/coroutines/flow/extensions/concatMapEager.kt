package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
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

/**
 * inner Flow를 eager하게 수집하되 동시 inner 수와 inner별 출력 queue 용량을 제한합니다.
 *
 * ## 동작/계약
 * - 최대 [maxConcurrency]개의 inner만 동시에 수집합니다.
 * - 각 inner는 [bufferCapacity] 용량의 `Channel`을 사용하며, queue가 가득 차면
 *   inner producer가 suspend됩니다. `0`은 rendezvous queue입니다.
 * - downstream 방출은 source 순서를 유지합니다. 앞선 inner가 느려도 뒤 inner의
 *   값은 자신의 bounded queue까지만 누적됩니다.
 * - transform/inner 실패는 원래 예외로 전달되고, 모든 child와 permit는
 *   structured cancellation과 `finally`에서 정리됩니다.
 *
 * ```kotlin
 * val out = source.concatMapEager(maxConcurrency = 4, bufferCapacity = 8) { load(it) }
 *     .toList()
 * ```
 *
 * @param maxConcurrency 동시에 수집할 inner Flow의 최대 개수입니다.
 * @param bufferCapacity inner별 출력 queue의 최대 용량입니다.
 * @param transform source 값을 inner Flow로 변환하는 함수입니다.
 */
fun <T: Any, R: Any> Flow<T>.concatMapEager(
    maxConcurrency: Int,
    bufferCapacity: Int = maxConcurrency,
    transform: suspend (T) -> Flow<R>,
): Flow<R> {
    require(maxConcurrency > 0) { "maxConcurrency must be positive" }
    require(bufferCapacity >= 0) { "bufferCapacity must be non-negative" }
    return concatMapEagerBoundedInternal(maxConcurrency, bufferCapacity, transform)
}

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
        while (true) {
            coroutineContext.ensureActive()
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

private fun <T: Any, R: Any> Flow<T>.concatMapEagerBoundedInternal(
    maxConcurrency: Int,
    bufferCapacity: Int,
    transform: suspend (T) -> Flow<R>,
): Flow<R> = channelFlow {
    coroutineScope {
        val resumeOutput = Resumable()
        val innerQueues = ConcurrentLinkedQueue<BoundedConcatMapEagerInnerQueue<R>>()
        val state = ConcatMapEagerState()
        val permits = Semaphore(maxConcurrency)

        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                collect { item ->
                    permits.acquire()
                    var releasePermit = true
                    try {
                        val inner = transform(item)
                        val queue = BoundedConcatMapEagerInnerQueue<R>(bufferCapacity)
                        innerQueues.offer(queue)
                        resumeOutput.resume()

                        launch {
                            var failure: Throwable? = null
                            try {
                                inner.collect { value ->
                                    queue.channel.send(value)
                                    resumeOutput.resume()
                                }
                            } catch (cause: Throwable) {
                                failure = cause
                                throw cause
                            } finally {
                                queue.channel.close(failure)
                                permits.release()
                                resumeOutput.resume()
                            }
                        }
                        releasePermit = false
                    } finally {
                        if (releasePermit) permits.release()
                    }
                }
            } finally {
                state.innerDone.value = true
                resumeOutput.resume()
            }
        }

        var current: BoundedConcatMapEagerInnerQueue<R>? = null
        while (true) {
            coroutineContext.ensureActive()
            if (current == null) {
                val done = state.innerDone.value
                current = innerQueues.poll()
                if (done && current == null) break
            }

            val queue = current
            if (queue != null) {
                val result = queue.channel.receiveCatching()
                if (result.isSuccess) {
                    send(result.getOrThrow())
                    continue
                }
                current = null
                result.exceptionOrNull()?.let { throw it }
                continue
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

private class BoundedConcatMapEagerInnerQueue<R: Any>(capacity: Int) {
    val channel = Channel<R>(capacity)
}
