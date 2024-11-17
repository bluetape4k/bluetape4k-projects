package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * 모든 소스 [Flow]들을 수집을 시도하는데, 첫 번째 요소를 발행하는 Flow를 collect 하고, 나머지 Flow들은 collect를 취소합니다.
 *
 * ```
 * val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }
 * val flow2 = flowRangeOf(6, 5).onStart { delay(100) }
 *
 * listOf(flow1, flow2).race()   // 6, 7, 8, 9, 10
 * ```
 *
 * @see [amb]
 */
fun <T> Iterable<Flow<T>>.race(): Flow<T> = amb()


/**
 * 모든 소스 [Flow]들을 수집을 시도하는데, 첫 번째 요소를 발행하는 Flow를 collect 하고, 나머지 Flow들은 collect를 취소합니다.
 *
 * ```
 * val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }
 * val flow2 = flowRangeOf(6, 5).onStart { delay(100) }
 *
 * race(flow1, flow2)  // 6, 7, 8, 9, 10
 * ```
 *
 * @see [amb]
 */
fun <T> race(flow1: Flow<T>, flow2: Flow<T>, vararg flows: Flow<T>): Flow<T> = amb(flow1, flow2, *flows)

/**
 * 모든 소스 [Flow]들을 수집을 시도하는데, 첫 번째 요소를 발행하는 Flow를 collect 하고, 나머지 Flow들은 collect를 취소합니다.
 *
 *
 * ```
 * val flow1 = flowRangeOf(1, 5).onStart { delay(1000) }
 * val flow2 = flowRangeOf(6, 5).onStart { delay(100) }
 *
 * flow1.raceWith(flow2)  // 6, 7, 8, 9, 10
 * ```
 *
 * @see [ambWith]
 */
fun <T> Flow<T>.raceWith(flow1: Flow<T>, vararg flows: Flow<T>): Flow<T> = ambWith(flow1, *flows)
