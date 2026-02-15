package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.yield

/**
 * 모든 소스 [Flow]들을 수집을 시도하는데, 첫 번째 요소를 발행하는 Flow를 collect 하고, 나머지 Flow들은 collect를 취소합니다.
 *
 * ```
 * val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }
 * val flow2 = flowRangeOf(6, 5).onStart { delay(100) }
 *
 * amb(flow1, flow2)  // 6, 7, 8, 9, 10
 * ```
 *
 * @see [race]
 */
fun <T> amb(flow1: Flow<T>, flow2: Flow<T>, vararg flows: Flow<T>): Flow<T> =
    ambInternal(
        buildList(capacity = 2 + flows.size) {
            add(flow1)
            add(flow2)
            addAll(flows)
        }
    )

/**
 * 모든 소스 [Flow]들을 수집을 시도하는데, 첫 번째 요소를 발행하는 Flow를 collect 하고, 나머지 Flow들은 collect를 취소합니다.
 *
 * ```
 * val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }
 * val flow2 = flowRangeOf(6, 5).onStart { delay(100) }
 *
 * listOf(flow1, flow2).amb()   // 6, 7, 8, 9, 10
 * ```
 *
 * @see [race]
 */
fun <T> Iterable<Flow<T>>.amb(): Flow<T> = ambInternal(this)

/**
 * 모든 소스 [Flow]들을 수집을 시도하는데, 첫 번째 요소를 발행하는 Flow를 collect 하고, 나머지 Flow들은 collect를 취소합니다.
 *
 * ```
 * val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }
 * val flow2 = flowRangeOf(6, 5).onStart { delay(100) }
 *
 * flow1.ambWith(flow2)  // 6, 7, 8, 9, 10
 * ```
 *
 * @see [raceWith]
 */
fun <T> Flow<T>.ambWith(flow1: Flow<T>, vararg flows: Flow<T>): Flow<T> = amb(this, flow1, *flows)

internal fun <T> ambInternal(sources: Iterable<Flow<T>>): Flow<T> = flow {
    coroutineScope {
        val channels = sources.map { flow ->
            // Produce the values using the default (rendezvous) channel
            produce {
                flow.collect {
                    send(it)
                    yield() // 모든 Flow가 공정하게 emit 할 기회를 주기 위해
                }
            }
        }

        if (channels.isEmpty()) {
            return@coroutineScope
        }

        channels
            .singleOrNull()
            ?.let { return@coroutineScope emitAll(it) }

        // "첫 신호(완료 포함)"가 아니라 "첫 값 emit"을 우승 기준으로 삼는다.
        val alive = channels.indices.toMutableSet()

        while (alive.isNotEmpty()) {
            val (winnerIndex, winnerResult) = select {
                alive.forEach { index ->
                    channels[index].onReceiveCatching {
                        index to it
                    }
                }
            }

            winnerResult
                .onSuccess {
                    channels.forEachIndexed { index, channel ->
                        if (index != winnerIndex) {
                            channel.cancel()
                        }
                    }

                    emit(it)
                    emitAll(channels[winnerIndex])
                    return@coroutineScope
                }
                .onFailure { cause ->
                    // 값 없이 종료된 source는 후보에서 제거하고 계속 경합한다.
                    if (cause == null) {
                        alive.remove(winnerIndex)
                    } else {
                        channels.forEachIndexed { index, channel ->
                            if (index != winnerIndex) {
                                channel.cancel(CancellationException("amb winner failed", cause))
                            }
                        }
                        throw cause
                    }
                }
        }
    }
}
