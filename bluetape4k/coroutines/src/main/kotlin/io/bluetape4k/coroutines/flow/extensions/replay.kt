package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.subject.ReplaySubject
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 값을 multicasts 합니다
 * 그리고 그 소비자들은 값을 출력 flow 로 생성할 수 있습니다.
 *
 * ```
 * flowRangeOf(1, 5)
 *     .replay { shared ->
 *         merge(
 *             shared.filter { it % 2 == 1 }.log("odd", log),
 *             shared.filter { it % 2 == 0 }.log("even", log)
 *         )
 *     }
 *     .assertResult(1, 3, 5, 2, 4)
 * ```
 *
 * @param transform 변환 함수
 */
fun <T, R> Flow<T>.replay(transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject() }, transform)

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 최대 [maxSize] 개수의 캐시된 값까지 multicasts 합니다
 * 그리고 그 소비자들은 값을 출력 flow 로 생성할 수 있습니다.
 *
 * ```
 * flowRangeOf(1, 5)
 *     .replay(2) { shared ->
 *         shared
 *             .log("filter")                          // 1,2,3,4,5
 *             .filter { it % 2 == 0 }                       // 2, 4
 *             .concatWith(shared.log("replay 2"))     // 4, 5 (replay : 마지막 2개)
 *     }
 *     .assertResult(2, 4, 4, 5)    // filters: 2, 4   || concatWith : 4, 5
 * ```
 *
 * @param maxSize 캐시할 최대 아이템 갯수
 * @param transform 변환 함수
 */
fun <T, R> Flow<T>.replay(maxSize: Int, transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject(maxSize) }, transform)

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 최대 [maxTimeout] 내의 캐시된 값까지 multicasts 합니다
 * 그리고 그 소비자들은 값을 출력 flow 로 생성할 수 있습니다.
 *
 * ```
 * flowRangeOf(1, 5)
 *     .onEach { delay(100) }
 *     .replay(timeout) { shared ->
 *         shared
 *             .log("filter")  // 1,2,3,4,5
 *             .filter { it % 2 == 0 }              // 2, 4
 *             .concatWith(shared.log("replay timeout[$timeout]"))  // 1,2,3,4,5 (replay : timeout 만)
 *     }
 *     .assertResult(2, 4, 1, 2, 3, 4, 5) // filters : 2, 4 || concatWith : 1,2,3,4,5
 * ```
 *
 * @param maxTimeout 캐시할 최대 시간
 * @param transform 변환 함수
 */
fun <T, R> Flow<T>.replay(maxTimeout: Duration, transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject(maxTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }, transform)

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 최대 [maxSize] 갯수 또는 최대 [maxTimeout] 내의 캐시된 값까지 multicasts 합니다
 * 그리고 그 소비자들은 값을 출력 flow 로 생성할 수 있습니다.
 *
 * ```
 * val timeout = 1.minutes
 * flowRangeOf(1, 5)
 *     .replay(2, timeout) { shared ->
 *         shared
 *             .log("filter")  // 1,2,3,4,5
 *             .filter { it % 2 == 0 }              // 2, 4
 *             .concatWith(shared.log("replay 2"))  // 4, 5 (replay : 마지막 2개)
 *     }
 *     .assertResult(2, 4, 4, 5)    // filters: 2, 4   || concatWith : 4, 5
 * ```
 *
 * @param maxSize 캐시할 최대 아이템 갯수
 * @param maxTimeout 캐시할 최대 시간
 * @param transform 변환 함수
 */
fun <T, R> Flow<T>.replay(maxSize: Int, maxTimeout: Duration, transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject(maxSize, maxTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }, transform)

/**
 * 하나의 collector 를 upstream source 로 공유하고,
 * 여러 소비자에게 최대 [maxSize] 갯수 또는 최대 [maxTimeout] 내의 캐시된 값까지 multicasts 합니다
 * 그리고 그 소비자들은 값을 출력 flow 로 생성할 수 있습니다.
 *
 * ```
 * val timeout = 1.minutes
 * val timeSource: (TimeUnit) -> Long = { System.currentTimeMillis() }
 * flowRangeOf(1, 5)
 *     .replay(2, timeout, timeSource) { shared ->
 *         shared
 *             .log("filter")
 *             .filter { it % 2 == 0 }                  // 2, 4
 *             .concatWith(shared.log("replay 2"))  // 4, 5 (replay : 마지막 2개)
 *     }
 *     .assertResult(2, 4, 4, 5)    // filters: 2, 4   || concatWith : 4, 5
 * ```
 *
 * @param maxSize 캐시할 최대 아이템 갯수
 * @param maxTimeout 캐시할 최대 시간
 * @param timeSource 시간을 제공하는 함수
 * @param transform 변환 함수
 */
fun <T, R> Flow<T>.replay(
    maxSize: Int,
    maxTimeout: Duration,
    timeSource: (TimeUnit) -> Long,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> =
    replay(
        { ReplaySubject(maxSize, maxTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS, timeSource) },
        transform
    )

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 값을 multicasts 합니다
 * 그리고 그 소비자들은 값을 출력 flow 로 생성할 수 있습니다.
 *
 * @param replaySubjectSupplier [ReplaySubject] 를 제공하는 함수
 * @param transform 변환 함수
 */
fun <T, R> Flow<T>.replay(
    replaySubjectSupplier: () -> ReplaySubject<T>,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> =
    multicastInternal(this, replaySubjectSupplier, transform)
