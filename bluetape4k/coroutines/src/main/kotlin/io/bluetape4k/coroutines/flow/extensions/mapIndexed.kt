package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow

/**
 * 각 요소에 인덱스를 함께 전달해 변환합니다.
 *
 * ## 동작/계약
 * - 인덱스는 collect 순서 기준으로 `0`부터 증가합니다.
 * - `transform(index, value)` 결과를 동일 순서로 방출합니다.
 * - 추가 버퍼 없이 순차 수집/순차 방출합니다.
 *
 * ```kotlin
 * val result = flowOf("a", "b").mapIndexed { i, v -> "$i:$v" }.toList()
 * // result == ["0:a", "1:b"]
 * ```
 *
 * @param transform 인덱스와 값을 받아 결과를 만드는 함수입니다.
 */
inline fun <T, R> Flow<T>.mapIndexed(
    crossinline transform: suspend (index: Int, value: T) -> R,
): Flow<R> = flow {
    this@mapIndexed.collectIndexed { index, value ->
        emit(transform(index, value))
    }
}
