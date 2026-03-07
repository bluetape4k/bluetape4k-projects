package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 변환 함수를 병렬로 실행해 결과를 병합합니다.
 *
 * ## 동작/계약
 * - `parallelism`은 최소 1로 보정한 뒤 `flatMapMerge(concurrency)`에 사용합니다.
 * - 보정된 값이 1이면 일반 `map` 경로를 사용해 불필요한 병합 오버헤드를 피합니다.
 * - 각 요소는 `flowFromSuspend { transform(value) }`로 감싸 병렬 실행됩니다.
 * - 결과 순서는 원본 순서와 달라질 수 있습니다.
 * - `context`는 최종 `flowOn(context)`으로 적용됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).mapParallel(parallelism = 2) { it * 10 }.toList()
 * // out은 [10, 20, 30]을 포함하되 순서는 달라질 수 있다.
 * ```
 *
 * @param parallelism 동시 변환 수입니다. 1 미만은 1로 보정됩니다.
 * @param context 병렬 변환 파이프라인에 적용할 코루틴 컨텍스트입니다.
 * @param transform 각 요소를 결과값으로 변환하는 suspend 함수입니다.
 */
inline fun <T, R> Flow<T>.mapParallel(
    parallelism: Int = DEFAULT_CONCURRENCY,
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (value: T) -> R,
): Flow<R> {
    val concurrency = parallelism.coerceAtLeast(1)

    if (concurrency == 1) {
        return map { value -> transform(value) }.flowOn(context)
    }

    return flatMapMerge(concurrency) { value ->
        flowFromSuspend { transform(value) }
    }.flowOn(context)
}
