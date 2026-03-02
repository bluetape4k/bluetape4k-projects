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
 * 여러 Flow 중 첫 값을 방출한 Flow를 winner로 선택합니다.
 *
 * ## 동작/계약
 * - winner 기준은 "첫 신호"가 아니라 "첫 값 emit"입니다.
 * - winner가 결정되면 나머지 source 채널은 취소됩니다.
 * - winner가 값 없이 정상 종료되면 후보에서 제거하고 다음 후보로 경합을 계속합니다.
 * - winner가 예외로 종료되면 해당 예외를 전파합니다.
 *
 * ```kotlin
 * val out = amb(flowOf(1, 2), flowOf(3, 4))
 * // out은 먼저 값을 보낸 한 source의 값만 방출
 * ```
 *
 * @param flow1 첫 번째 후보 Flow입니다.
 * @param flow2 두 번째 후보 Flow입니다.
 * @param flows 추가 후보 Flow들입니다.
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
 * 컬렉션으로 받은 Flow 후보들에 대해 amb 경쟁을 수행합니다.
 *
 * ## 동작/계약
 * - 빈 입력이면 즉시 완료합니다.
 * - 단일 입력이면 해당 Flow를 그대로 방출합니다.
 * - 2개 이상이면 [ambInternal] 경합 로직을 수행합니다.
 */
fun <T> Iterable<Flow<T>>.amb(): Flow<T> = ambInternal(this)

/**
 * 수신 Flow를 포함해 amb 경쟁을 수행합니다.
 *
 * ## 동작/계약
 * - 수신 Flow, `flow1`, `flows`를 묶어 [amb]에 위임합니다.
 *
 * @param flow1 추가 후보 Flow입니다.
 * @param flows 추가 후보 Flow들입니다.
 */
fun <T> Flow<T>.ambWith(flow1: Flow<T>, vararg flows: Flow<T>): Flow<T> = amb(this, flow1, *flows)

internal fun <T> ambInternal(sources: Iterable<Flow<T>>): Flow<T> = flow {
    coroutineScope {
        val channels = sources.map { flow ->
            produce {
                flow.collect {
                    send(it)
                    yield()
                }
            }
        }

        if (channels.isEmpty()) {
            return@coroutineScope
        }

        channels
            .singleOrNull()
            ?.let { return@coroutineScope emitAll(it) }

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
