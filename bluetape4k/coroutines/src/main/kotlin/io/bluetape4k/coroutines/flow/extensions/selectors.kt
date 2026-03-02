@file:Suppress("UNCHECKED_CAST")

package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.utils.NULL_VALUE
import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 상태에서 부분 상태를 추출하는 suspend 선택 함수 타입입니다.
 *
 * ## 동작/계약
 * - 입력 상태를 받아 파생 값을 계산합니다.
 * - suspend 함수이므로 선택 과정에서 비동기 작업을 포함할 수 있습니다.
 * - 선택 함수 자체는 상태를 변경하지 않는 순수 함수로 사용하는 것을 권장합니다.
 *
 * ```kotlin
 * val selector: Selector<Int, Int> = { it % 2 }
 * // selector(3) == 1
 * ```
 */
typealias Selector<State, SubState> = suspend (State) -> SubState

/**
 * 단일 selector 결과를 distinct하게 방출합니다.
 *
 * ## 동작/계약
 * - 각 상태에서 [selector]를 적용한 결과를 계산합니다.
 * - 연속 중복 값은 `distinctUntilChanged()`로 제거됩니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 10, 11).select { it.toString().length }.toList()
 * // out == [1, 2]
 * ```
 * @param selector 상태에서 결과 값을 추출하는 함수입니다.
 */
fun <State, Result> Flow<State>.select(selector: Selector<State, Result>): Flow<Result> =
    map(selector).distinctUntilChanged()

/**
 * 2개 selector 조합 결과를 distinct하게 방출합니다.
 *
 * ## 동작/계약
 * - 각 상태에서 두 selector 값을 계산하고 [projector]로 최종 값을 만듭니다.
 * - selector 입력 배열이 이전과 동일하면 projector 호출을 생략합니다.
 * - projector 결과도 직전 값과 같으면 emit하지 않습니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .select({ it % 2 }, { it > 1 }) { a, b -> "$a-$b" }
 *     .toList()
 * // out == ["1-false", "0-true", "1-true"]
 * ```
 * @param selector1 첫 번째 부분 상태 selector입니다.
 * @param selector2 두 번째 부분 상태 selector입니다.
 * @param projector 선택된 부분 상태를 최종 결과로 조합하는 함수입니다.
 */
fun <State, SubState1, SubState2, Result> Flow<State>.select(
    selector1: Selector<State, SubState1>,
    selector2: Selector<State, SubState2>,
    projector: suspend (SubState1, SubState2) -> Result,
): Flow<Result> = selectInternal(
    selectors = arrayOf(selector1, selector2),
    projector = { projector(it[0] as SubState1, it[1] as SubState2) }
)

/**
 * 3개 selector 조합 결과를 distinct하게 방출합니다.
 *
 * ## 동작/계약
 * - 각 상태에서 3개 selector 값을 계산해 [projector]로 조합합니다.
 * - selector 입력 조합 또는 projector 결과가 이전과 동일하면 emit하지 않습니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .select({ it }, { it % 2 }, { it > 1 }) { a, b, c -> "$a-$b-$c" }
 *     .toList()
 * // out == ["1-1-false", "2-0-true", "3-1-true"]
 * ```
 * @param selector1 첫 번째 부분 상태 selector입니다.
 * @param selector2 두 번째 부분 상태 selector입니다.
 * @param selector3 세 번째 부분 상태 selector입니다.
 * @param projector 선택된 부분 상태를 최종 결과로 조합하는 함수입니다.
 */
fun <State, SubState1, SubState2, SubState3, Result> Flow<State>.select(
    selector1: Selector<State, SubState1>,
    selector2: Selector<State, SubState2>,
    selector3: Selector<State, SubState3>,
    projector: suspend (SubState1, SubState2, SubState3) -> Result,
): Flow<Result> = selectInternal(
    selectors = arrayOf(selector1, selector2, selector3),
    projector = {
        projector(
            it[0] as SubState1,
            it[1] as SubState2,
            it[2] as SubState3
        )
    }
)

/**
 * 4개 selector 조합 결과를 distinct하게 방출합니다.
 *
 * ## 동작/계약
 * - 각 상태에서 4개 selector 값을 계산해 [projector]로 조합합니다.
 * - selector 입력 조합 또는 projector 결과가 이전과 동일하면 emit하지 않습니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2)
 *     .select({ it }, { it }, { it }, { it }) { a, b, c, d -> a + b + c + d }
 *     .toList()
 * // out == [4, 8]
 * ```
 * @param selector1 첫 번째 부분 상태 selector입니다.
 * @param selector2 두 번째 부분 상태 selector입니다.
 * @param selector3 세 번째 부분 상태 selector입니다.
 * @param selector4 네 번째 부분 상태 selector입니다.
 * @param projector 선택된 부분 상태를 최종 결과로 조합하는 함수입니다.
 */
fun <State, SubState1, SubState2, SubState3, SubState4, Result> Flow<State>.select(
    selector1: Selector<State, SubState1>,
    selector2: Selector<State, SubState2>,
    selector3: Selector<State, SubState3>,
    selector4: Selector<State, SubState4>,
    projector: suspend (SubState1, SubState2, SubState3, SubState4) -> Result,
): Flow<Result> = selectInternal(
    selectors = arrayOf(selector1, selector2, selector3, selector4),
    projector = {
        projector(
            it[0] as SubState1,
            it[1] as SubState2,
            it[2] as SubState3,
            it[3] as SubState4,
        )
    }
)

/**
 * 5개 selector 조합 결과를 distinct하게 방출합니다.
 *
 * ## 동작/계약
 * - 각 상태에서 5개 selector 값을 계산해 [projector]로 조합합니다.
 * - selector 입력 조합 또는 projector 결과가 이전과 동일하면 emit하지 않습니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2)
 *     .select({ it }, { it }, { it }, { it }, { it }) { a, b, c, d, e -> a + b + c + d + e }
 *     .toList()
 * // out == [5, 10]
 * ```
 * @param selector1 첫 번째 부분 상태 selector입니다.
 * @param selector2 두 번째 부분 상태 selector입니다.
 * @param selector3 세 번째 부분 상태 selector입니다.
 * @param selector4 네 번째 부분 상태 selector입니다.
 * @param selector5 다섯 번째 부분 상태 selector입니다.
 * @param projector 선택된 부분 상태를 최종 결과로 조합하는 함수입니다.
 */
fun <State, SubState1, SubState2, SubState3, SubState4, SubState5, Result> Flow<State>.select(
    selector1: Selector<State, SubState1>,
    selector2: Selector<State, SubState2>,
    selector3: Selector<State, SubState3>,
    selector4: Selector<State, SubState4>,
    selector5: Selector<State, SubState5>,
    projector: suspend (SubState1, SubState2, SubState3, SubState4, SubState5) -> Result,
): Flow<Result> = selectInternal(
    selectors = arrayOf(selector1, selector2, selector3, selector4, selector5),
    projector = {
        projector(
            it[0] as SubState1,
            it[1] as SubState2,
            it[2] as SubState3,
            it[3] as SubState4,
            it[4] as SubState5,
        )
    }
)

private typealias SubStateT = Any?

private fun <State, Result> Flow<State>.selectInternal(
    selectors: Array<Selector<State, SubStateT>>,
    projector: suspend (Array<SubStateT>) -> Result,
): Flow<Result> {
    selectors.requireNotEmpty("selectors")

    return flow {
        var latestSubStates: Array<SubStateT>? = null
        var latestState: Any? = NULL_VALUE
        var reusableSubStates: Array<SubStateT>? = null

        collect { state ->
            val currentSubStates =
                reusableSubStates ?: arrayOfNulls<SubStateT>(selectors.size).also { reusableSubStates = it }

            selectors.indices.forEach {
                currentSubStates[it] = selectors[it](state)
            }

            if (latestSubStates === null || !currentSubStates.contentEquals(latestSubStates)) {
                val currentState = projector(
                    currentSubStates.copyOf().also { latestSubStates = it }
                )
                if (latestState === NULL_VALUE || (latestState as Result) != currentState) {
                    latestState = currentState
                    emit(currentState)
                }
            }
        }
    }
}
