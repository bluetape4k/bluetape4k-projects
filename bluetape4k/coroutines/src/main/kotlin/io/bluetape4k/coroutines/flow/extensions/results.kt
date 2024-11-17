package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [Flow]의 값을 [성공 결과][Result.success]로 매핑하고, 예외를 catch하여 [실패 결과][Result.failure]로 래핑합니다.
 *
 * ```
 * flowOf(1, 2, 3)
 *     .mapToResult()
 *     .test {
 *         awaitItem() shouldBeEqualTo Result.success(1)
 *         awaitItem() shouldBeEqualTo Result.success(2)
 *         awaitItem() shouldBeEqualTo Result.success(3)
 *         awaitComplete()
 *     }
 * ```
 *
 * ```
 * flow {
 *     emit(1)
 *     throw testException
 * }
 *     .mapToResult()
 *     .test {
 *         awaitItem() shouldBeEqualTo Result.success(1)
 *         awaitItem() shouldBeEqualTo Result.failure(testException)
 *         awaitComplete()
 *     }
 * ```
 */
fun <T> Flow<T>.mapToResult(): Flow<Result<T>> =
    map { Result.success(it) }
        .catchAndReturn { Result.failure(it) }

/**
 * [Result]의 Flow를 [transform]으로 매핑하여 [Result]의 Flow를 생성합니다.
 *
 * [transform] 함수에서 throw된 예외는 catch되어 결과 플로우로 [실패 결과][Result.failure]로 emit됩니다.
 *
 * ```
 * flowOf(1, 2, 3)
 *     .mapToResult()
 *     .mapResultCatching { it * 2 }
 *     .test {
 *         awaitItem() shouldBeEqualTo Result.success(2)
 *         awaitItem() shouldBeEqualTo Result.success(4)
 *         awaitItem() shouldBeEqualTo Result.success(6)
 *         awaitComplete()
 *     }
 * ```
 *
 * ```
 * flow {
 *     emit(1)
 *     throw testException
 * }
 *     .mapToResult()
 *     .mapResultCatching { it * 2 }
 *     .test {
 *         awaitItem() shouldBeEqualTo Result.success(2)
 *         awaitItem() shouldBeEqualTo Result.failure(testException)
 *         awaitComplete()
 *     }
 * ```
 *
 * @see Result.mapCatching
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
 * 요소인 [Result]가 [Result.Failure]인 경우 예외로 다시 던집니다.
 *
 * ```
 * flowOf(1, 2, 3)
 *     .mapToResult()
 *     .throwFailure()
 *     .test {
 *         awaitItem() shouldBeEqualTo 1
 *         awaitItem() shouldBeEqualTo 2
 *         awaitItem() shouldBeEqualTo 3
 *         awaitComplete()
 *     }
 * ```
 *
 * ```
 * flow {
 *     emit(1)
 *     throw testException
 * }
 *     .mapToResult()
 *     .throwFailure()
 *     .test {
 *         awaitItem() shouldBeEqualTo 1
 *         awaitError() shouldBeEqualTo testException
 *     }
 * ```
 *
 * @see Result.getOrThrow
 */
fun <T> Flow<Result<T>>.throwFailure(): Flow<T> =
    map { it.getOrThrow() }


/**
 * [Result]의 Flow를 매핑하여 [Flow]로 변환합니다. 실패한 결과는 [defaultValue]로 대체됩니다.
 *
 * ```
 * flow {
 *     emit(1)
 *     throw testException
 * }
 *     .mapToResult()
 *     .getOrDefault(-1)
 *     .test {
 *         awaitItem() shouldBeEqualTo 1
 *         awaitItem() shouldBeEqualTo -1
 *         awaitComplete()
 *     }
 * ```
 *
 * @see Result.getOrDefault
 */
fun <T> Flow<Result<T>>.getOrDefault(defaultValue: T): Flow<T> =
    map { it.getOrDefault(defaultValue) }
