package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.util.concurrent.ConcurrentLinkedQueue

private val log: Logger by lazy { KotlinLogging.logger { } }

/**
 * 여러 Flow를 eager하게 동시 수집하되 source 순서대로 연결해 방출합니다.
 *
 * ## 동작/계약
 * - 모든 source를 즉시 수집 시작합니다.
 * - 각 source의 값은 source별 큐에 적재되고, 출력은 source 인덱스 순서대로 소비됩니다.
 * - 앞선 source 큐가 비어 있고 아직 완료되지 않았으면 뒤 source 값이 준비돼도 대기합니다.
 * - source 수만큼 큐/완료 플래그가 할당됩니다.
 *
 * ```kotlin
 * val result = listOf(flowOf(1, 2), flowOf(3, 4)).concatFlows().toList()
 * // result == [1, 2, 3, 4]
 * ```
 */
fun <T: Any> Iterable<Flow<T>>.concatFlows(): Flow<T> =
    concatArrayEagerInternal(this.toList())

/**
 * `Flow<Flow<T>>`를 리스트로 수집한 뒤 eager concat을 수행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `toList()`로 모든 inner Flow를 먼저 모은 후 [concatArrayEagerInternal]에 전달합니다.
 * - 따라서 source Flow가 완료되기 전에는 결과 방출이 시작되지 않습니다.
 */
suspend fun <T: Any> Flow<Flow<T>>.concatFlows(): Flow<T> =
    concatArrayEagerInternal(this.toList())

/**
 * vararg Flow들에 대해 eager concat을 수행합니다.
 *
 * ## 동작/계약
 * - 입력 배열을 리스트로 변환해 [concatArrayEagerInternal]에 위임합니다.
 * - 입력이 비어 있으면 빈 Flow를 반환합니다.
 *
 * @param sources 순서대로 연결할 source Flow들입니다.
 */
fun <T: Any> concatArrayEager(vararg sources: Flow<T>): Flow<T> =
    concatArrayEagerInternal(sources.asList())

@Suppress("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN")
internal fun <T: Any> concatArrayEagerInternal(sources: List<Flow<T>>): Flow<T> = channelFlow {
    coroutineScope {
        val size = sources.size
        val rails = List(size) { ConcatArrayEagerRail<T>() }
        val reader = Resumable()

        repeat(size) {
            val f = sources[it]
            val rail = rails[it]
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    f.collect { item ->
                        log.trace { "collect from source[$it] item=$item" }
                        rail.queue.offer(item)
                        reader.resume()
                    }
                } finally {
                    rail.done.value = true
                    reader.resume()
                }
            }
        }

        var index = 0
        while (isActive && index < size) {
            val rail = rails[index]
            val done = rail.done.value

            if (done && rail.queue.isEmpty()) {
                index++
                continue
            }
            val value = rail.queue.poll()
            if (value != null) {
                send(value)
                continue
            }
            reader.await()
        }
    }
}

private class ConcatArrayEagerRail<T: Any> {
    val queue = ConcurrentLinkedQueue<T>()
    val done = atomic(false)
}
