package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.subject.ReplaySubject
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * 기본(무제한 버퍼) replay subject를 사용해 공유 변환을 수행합니다.
 *
 * ## 동작/계약
 * - 업스트림을 ReplaySubject로 멀티캐스트한 뒤 [transform]에 전달합니다.
 * - [transform] 내부에서 `shared`를 재구독하면 과거 값이 replay 됩니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(1, 5).replay { shared -> shared.filter { it % 2 == 0 } }.toList()
 * // out == [2, 4]
 * ```
 * @param transform replay 공유 Flow를 이용해 최종 Flow를 구성하는 함수입니다.
 */
fun <T, R> Flow<T>.replay(transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject() }, transform)

/**
 * 크기 제한 replay subject를 사용해 공유 변환을 수행합니다.
 *
 * ## 동작/계약
 * - 최근 [maxSize]개 값만 replay 대상으로 유지합니다.
 * - [maxSize]의 보정 규칙은 [ReplaySubject] 구현을 따릅니다(`<=0`이면 1로 보정).
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(1, 5).replay(2) { shared ->
 *     shared.filter { it % 2 == 0 }.concatWith(shared)
 * }.toList()
 * // out == [2, 4, 4, 5]
 * ```
 * @param maxSize replay에 유지할 최대 값 개수입니다.
 * @param transform replay 공유 Flow를 이용해 최종 Flow를 구성하는 함수입니다.
 */
fun <T, R> Flow<T>.replay(maxSize: Int, transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject(maxSize) }, transform)

/**
 * 시간 제한 replay subject를 사용해 공유 변환을 수행합니다.
 *
 * ## 동작/계약
 * - 최근 [maxTimeout] 시간 윈도우에 있는 값만 replay 대상으로 유지합니다.
 * - 내부에서는 밀리초 단위로 변환해 subject를 생성합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(1, 5).replay(1.minutes) { shared ->
 *     shared.filter { it % 2 == 0 }.concatWith(shared)
 * }.toList()
 * // out == [2, 4, 1, 2, 3, 4, 5]
 * ```
 * @param maxTimeout replay에 유지할 최대 시간입니다.
 * @param transform replay 공유 Flow를 이용해 최종 Flow를 구성하는 함수입니다.
 */
fun <T, R> Flow<T>.replay(maxTimeout: Duration, transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject(maxTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }, transform)

/**
 * 크기+시간 제한 replay subject를 사용해 공유 변환을 수행합니다.
 *
 * ## 동작/계약
 * - 최근 [maxSize]개이면서 [maxTimeout] 시간 조건을 만족하는 값만 replay 합니다.
 * - 보정 규칙(`maxSize`, `maxTimeout`)은 [ReplaySubject] 구현을 따릅니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(1, 5).replay(2, 1.minutes) { shared ->
 *     shared.filter { it % 2 == 0 }.concatWith(shared)
 * }.toList()
 * // out == [2, 4, 4, 5]
 * ```
 * @param maxSize replay에 유지할 최대 값 개수입니다.
 * @param maxTimeout replay에 유지할 최대 시간입니다.
 * @param transform replay 공유 Flow를 이용해 최종 Flow를 구성하는 함수입니다.
 */
fun <T, R> Flow<T>.replay(maxSize: Int, maxTimeout: Duration, transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    replay({ ReplaySubject(maxSize, maxTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }, transform)

/**
 * 크기+시간 제한 replay subject를 사용자 정의 시간 소스로 생성해 공유 변환을 수행합니다.
 *
 * ## 동작/계약
 * - 시간 판정에 [timeSource] 결과를 사용합니다.
 * - 테스트에서 가상 시간 소스를 주입해 재현 가능한 replay 조건을 만들 수 있습니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(1, 5).replay(2, 1.minutes, { 0L }) { shared ->
 *     shared.filter { it % 2 == 0 }.concatWith(shared)
 * }.toList()
 * // out == [2, 4, 4, 5]
 * ```
 * @param maxSize replay에 유지할 최대 값 개수입니다.
 * @param maxTimeout replay에 유지할 최대 시간입니다.
 * @param timeSource 현재 시각을 반환하는 함수입니다.
 * @param transform replay 공유 Flow를 이용해 최종 Flow를 구성하는 함수입니다.
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
 * 사용자 정의 [ReplaySubject] 생성기로 replay 공유 변환을 수행합니다.
 *
 * ## 동작/계약
 * - [replaySubjectSupplier]가 생성한 subject를 멀티캐스트 채널로 사용합니다.
 * - subject 버퍼 정책/예외 전파/완료 동작은 supplier가 만든 subject 구현을 따릅니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowRangeOf(1, 5).replay({ ReplaySubject(2) }) { shared ->
 *     shared.filter { it % 2 == 0 }.concatWith(shared)
 * }.toList()
 * // out == [2, 4, 4, 5]
 * ```
 * @param replaySubjectSupplier replay subject 생성 함수입니다.
 * @param transform replay 공유 Flow를 이용해 최종 Flow를 구성하는 함수입니다.
 */
fun <T, R> Flow<T>.replay(
    replaySubjectSupplier: () -> ReplaySubject<T>,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> =
    multicastInternal(this, replaySubjectSupplier, transform)
