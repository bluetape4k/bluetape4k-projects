package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * flow의 요소들을 [parallelism]만큼 병렬로 [transform]을 수행하여 처리속도를 높힙니다.
 *
 * ```
 * val ranges = flowRangeOf(1, 20)
 *
 * ranges
 *     .onEach { delay(10) }
 *     .mapParallel {
 *         delay(Random.nextLong(10))
 *         it
 *     }
 *     .assertResultSet(ranges.toList())
 * ```
 *
 * @param parallelism 동시 실행할 숫자
 * @param context Coroutine Context
 * @param transform  변환 함수
 */
inline fun <T, R> Flow<T>.mapParallel(
    parallelism: Int = DEFAULT_CONCURRENCY,
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (value: T) -> R,
): Flow<R> {
    val concurrency = parallelism.coerceAtLeast(1)

    return flatMapMerge(concurrency) { value ->
        flowFromSuspend { transform(value) }
    }.flowOn(context)
}
