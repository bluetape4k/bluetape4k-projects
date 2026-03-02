package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * 여러 Flow 중 먼저 신호를 보내는 Flow 하나를 선택해 방출합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [amb]를 호출해 race 전략을 적용합니다.
 * - 선택되지 않은 나머지 Flow는 취소됩니다.
 * - 입력 컬렉션이 비어 있으면 `amb()`의 계약(예외/완료)을 따릅니다.
 *
 * ```kotlin
 * val winner = listOf(flowOf(1), flowOf(2)).race()
 * // winner는 먼저 emit한 한쪽 값만 방출
 * ```
 */
fun <T> Iterable<Flow<T>>.race(): Flow<T> = amb()

/**
 * 두 개 이상의 Flow에 대해 race 전략을 적용합니다.
 *
 * ## 동작/계약
 * - [amb]와 동일하게 가장 먼저 신호를 보낸 Flow만 선택됩니다.
 * - `flow1`, `flow2`는 필수이며 `flows`는 추가 후보입니다.
 *
 * ```kotlin
 * val out = race(flowOf(1), flowOf(2), flowOf(3))
 * // out은 가장 먼저 도착한 소스의 값만 방출
 * ```
 *
 * @param flow1 첫 번째 후보 Flow입니다.
 * @param flow2 두 번째 후보 Flow입니다.
 * @param flows 추가 후보 Flow들입니다.
 */
fun <T> race(flow1: Flow<T>, flow2: Flow<T>, vararg flows: Flow<T>): Flow<T> = amb(flow1, flow2, *flows)

/**
 * 수신 Flow와 다른 Flow들을 race로 결합합니다.
 *
 * ## 동작/계약
 * - 수신 Flow를 포함한 후보들에 대해 [ambWith]를 적용합니다.
 * - 가장 먼저 신호를 보낸 Flow만 최종 방출을 담당합니다.
 *
 * ```kotlin
 * val out = flowOf(1).raceWith(flowOf(2), flowOf(3))
 * // out은 가장 먼저 도착한 한 소스만 통과
 * ```
 *
 * @param flow1 추가 후보 Flow입니다.
 * @param flows 추가 후보 Flow들입니다.
 */
fun <T> Flow<T>.raceWith(flow1: Flow<T>, vararg flows: Flow<T>): Flow<T> = ambWith(flow1, *flows)
