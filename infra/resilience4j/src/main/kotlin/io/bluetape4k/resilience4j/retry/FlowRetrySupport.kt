package io.bluetape4k.resilience4j.retry

import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// resilience4j 에서 제공하는 Flow 용 retry 는 flow 를 제공해주는 emitter 의 예외 시에 retry 를 해주는 것이다.
// 여기서는 collect 시에 예외가 발생하는 경우에 retry 를 해주는 것이다.

/**
 * [Flow] 를 수집 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * flow.collectWithRetry(retry) {
 *     // 수집한 데이터 처리
 *     println(it)
 * }
 * ```
 *
 * @param retry Retry 설정
 * @param collector 수집할 데이터 처리
 */
suspend inline fun <T> Flow<T>.collectWithRetry(
    retry: Retry,
    crossinline collector: suspend (T) -> Unit,
) {
    val decorated = retry.decorateSuspendFunction1(collector)
    onEach { decorated(it) }.collect()
}

/**
 * [Flow] 를 수집 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * flow.mapWithRetry(retry) {
 *     // 데이터 변환
 *     it * 2
 * }
 * ```
 *
 * @param retry Retry 설정
 * @param mapper 데이터 변환
 * @return 변환된 데이터
 */
inline fun <T, R> Flow<T>.mapWithRetry(
    retry: Retry,
    crossinline mapper: suspend (T) -> R,
): Flow<R> {
    val decorated = retry.decorateSuspendFunction1(mapper)
    return map(decorated)
}

/**
 * [Flow] 를 수집 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * flow.onEachWithRetry(retry) {
 *     // 데이터 처리
 *     println(it)
 * }
 * ```
 *
 * @param retry Retry 설정
 * @param consumer 데이터 처리
 * @return 처리된 데이터
 */
inline fun <T> Flow<T>.onEachWithRetry(
    retry: Retry,
    crossinline consumer: suspend (T) -> Unit,
): Flow<T> {
    val decorated = retry.decorateSuspendFunction1(consumer)
    return onEach(decorated)
}
