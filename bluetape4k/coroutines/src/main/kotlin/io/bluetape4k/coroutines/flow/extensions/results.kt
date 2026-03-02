package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Flow 요소를 `Result.success`로 감싸고 upstream 예외를 `Result.failure`로 변환합니다.
 *
 * ## 동작/계약
 * - 정상 요소는 `Result.success(value)`로 방출됩니다.
 * - upstream에서 예외가 발생하면 `Result.failure(exception)` 1개를 방출하고 종료합니다.
 * - 취소 예외도 upstream에서 발생하면 failure로 캡처될 수 있으므로 사용 위치를 주의해야 합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2).mapToResult().toList()
 * // result == [Success(1), Success(2)]
 * ```
 */
fun <T> Flow<T>.mapToResult(): Flow<Result<T>> =
    map { Result.success(it) }
        .catchAndReturn { Result.failure(it) }

/**
 * `Result` 성공 값에만 변환을 적용하고 실패는 유지합니다.
 *
 * ## 동작/계약
 * - 각 요소에 대해 `mapCatching`을 수행합니다.
 * - transform 중 `CancellationException`이 발생하면 failure로 감싸지 않고 즉시 재전파합니다.
 * - 기타 예외는 `Result.failure`로 변환됩니다.
 *
 * ```kotlin
 * val result = flowOf(Result.success(2))
 *   .mapResultCatching { it * 10 }
 *   .toList()
 * // result == [Success(20)]
 * ```
 *
 * @param transform 성공 값 변환 함수입니다.
 */
fun <T, R> Flow<Result<T>>.mapResultCatching(transform: suspend (T) -> R): Flow<Result<R>> =
    map { result ->
        result
            .mapCatching { transform(it) }
            .onFailure {
                if (it is CancellationException) {
                    throw it
                }
            }
    }

/**
 * `Result.failure`를 예외로 다시 던지고 성공 값만 방출합니다.
 *
 * ## 동작/계약
 * - 각 요소에 대해 `getOrThrow()`를 호출합니다.
 * - 실패 결과를 만나면 즉시 예외가 전파되어 Flow가 종료됩니다.
 *
 * ```kotlin
 * val result = flowOf(Result.success(1), Result.success(2)).throwFailure().toList()
 * // result == [1, 2]
 * ```
 */
fun <T> Flow<Result<T>>.throwFailure(): Flow<T> =
    map { it.getOrThrow() }

/**
 * `Result`에서 성공 값 또는 기본값을 꺼내 방출합니다.
 *
 * ## 동작/계약
 * - 성공이면 원래 값을, 실패면 `defaultValue`를 방출합니다.
 * - 실패를 예외로 전파하지 않고 값으로 흡수합니다.
 *
 * ```kotlin
 * val result = flowOf(Result.success(1), Result.failure<Int>(RuntimeException()))
 *   .getOrDefault(-1)
 *   .toList()
 * // result == [1, -1]
 * ```
 *
 * @param defaultValue 실패 시 사용할 기본값입니다.
 */
fun <T> Flow<Result<T>>.getOrDefault(defaultValue: T): Flow<T> =
    map { it.getOrDefault(defaultValue) }
