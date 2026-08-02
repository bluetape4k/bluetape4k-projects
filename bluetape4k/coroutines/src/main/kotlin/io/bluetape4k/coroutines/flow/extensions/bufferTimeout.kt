package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.whileSelect
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration

private const val DEFAULT_BUFFER_TIMEOUT_CAPACITY = 16

/**
 * 최대 원소 수 또는 timeout 중 먼저 도달한 경계로 원소를 batch로 묶습니다.
 *
 * ## 동작/계약
 * - `maxSize`와 [timeout]은 양수여야 합니다.
 * - timer는 구독 시점이 아니라 첫 원소가 현재 batch에 들어온 시점에 시작합니다.
 * - count 또는 timeout 경계에 도달하면 비어 있지 않은 새 리스트를 방출합니다.
 * - 정상 완료 시 마지막 부분 batch를 방출하고, upstream 실패 시 진행 중인
 *   부분 batch를 버린 뒤 원래 예외를 전달합니다.
 * - 수신 clause를 timeout clause보다 먼저 등록하므로 같은 virtual time에 값과
 *   timeout이 동시에 준비되면 값이 우선합니다.
 * - downstream 취소는 upstream producer와 timer를 함께 취소하며
 *   `CancellationException`을 일반 오류로 바꾸지 않습니다.
 *
 * ```kotlin
 * val batches = source.bufferTimeout(maxSize = 100, timeout = 1.seconds).toList()
 * // count/time 경계로 닫힌 비어 있지 않은 List<Int> batch
 * ```
 *
 * @param maxSize 한 batch에 허용하는 최대 원소 수입니다.
 * @param timeout 첫 원소부터 batch를 유지할 최대 시간입니다.
 */
fun <T> Flow<T>.bufferTimeout(maxSize: Int, timeout: Duration): Flow<List<T>> =
    countOrTimeout(maxSize, timeout)

/**
 * 최대 원소 수 또는 timeout 중 먼저 도달한 경계로 원소를 window snapshot으로 묶습니다.
 *
 * 각 방출 window는 이미 완료된 리스트를 감싼 repeatable cold [Flow]입니다.
 * 따라서 같은 window를 여러 번 수집해도 동일한 snapshot을 재생하며, live
 * single-consumer window로 동작하지 않습니다. 그 밖의 완료·실패·취소 계약은
 * [bufferTimeout]과 같습니다.
 *
 * ```kotlin
 * val windows = source.windowTimeout(maxSize = 100, timeout = 1.seconds).toList()
 * val first = windows.first().toList()
 * ```
 *
 * @param maxSize 한 window에 허용하는 최대 원소 수입니다.
 * @param timeout 첫 원소부터 window를 유지할 최대 시간입니다.
 */
fun <T> Flow<T>.windowTimeout(maxSize: Int, timeout: Duration): Flow<Flow<T>> =
    countOrTimeout(maxSize, timeout).map { it.asFlow() }

private fun <T> Flow<T>.countOrTimeout(maxSize: Int, timeout: Duration): Flow<List<T>> = flow {
    require(maxSize > 0) { "maxSize must be positive" }
    require(timeout.isPositive()) { "timeout must be positive" }

    supervisorScope {
        val input = this@countOrTimeout.produceIn(this)
        try {
            var current = ArrayList<T>(minOf(maxSize, DEFAULT_BUFFER_TIMEOUT_CAPACITY))

            whileSelect {
                input.onReceiveCatching { result ->
                    result
                        .onSuccess { value ->
                            current += value
                            if (current.size == maxSize) {
                                emit(current)
                                current = ArrayList(minOf(maxSize, DEFAULT_BUFFER_TIMEOUT_CAPACITY))
                            }
                        }
                        .onFailure { cause ->
                            cause?.let { throw it }
                            if (current.isNotEmpty()) {
                                emit(current)
                                current = ArrayList(minOf(maxSize, DEFAULT_BUFFER_TIMEOUT_CAPACITY))
                            }
                        }
                        .isSuccess
                }
                if (current.isNotEmpty()) {
                    onTimeout(timeout) {
                        emit(current)
                        current = ArrayList(minOf(maxSize, DEFAULT_BUFFER_TIMEOUT_CAPACITY))
                        true
                    }
                }
            }
            if (current.isNotEmpty()) emit(current)
        } finally {
            input.cancel()
        }
    }
}
