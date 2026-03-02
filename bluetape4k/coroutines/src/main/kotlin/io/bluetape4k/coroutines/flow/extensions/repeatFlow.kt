package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.assertPositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 지정 횟수만큼 suspend 액션을 실행해 결과를 순차 방출합니다.
 *
 * ## 동작/계약
 * - `times.assertPositiveNumber("times")`를 검증하며 0 이하이면 assertion 위반이 발생합니다(`-ea` 필요).
 * - `action`은 `index = 0..times-1` 순서로 정확히 `times`번 호출됩니다.
 * - 수신 객체를 변경하지 않고 결과 요소만 순차 방출합니다.
 *
 * ```kotlin
 * val result = repeatFlow(3) { it * 10 }.toList()
 * // result == [0, 10, 20]
 * ```
 *
 * @param times 액션 반복 횟수입니다. 0 이하면 assertion 위반이 발생할 수 있습니다.
 * @param action 인덱스를 받아 방출할 값을 계산하는 suspend 함수입니다.
 */
inline fun <T> repeatFlow(
    times: Int,
    crossinline action: suspend (index: Int) -> T,
): Flow<T> = flow {
    times.assertPositiveNumber("times")

    repeat(times) { index ->
        emit(action(index))
    }
}
