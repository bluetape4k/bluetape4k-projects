package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.subject.MulticastSubject
import io.bluetape4k.coroutines.flow.extensions.subject.PublishSubject
import kotlinx.coroutines.flow.Flow

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 값을 multicasts 합니다
 *
 * **주의**
 *
 * **coroutines/[Flow]가 구현된 방식때문에, 이 기능을 보장하지 않습니다**
 *
 * [transform] 함수는 upstream과 downstream을 연결하는 시간이 보장되지 않기 때문에,
 * upstream 아이템이 수집되고 변환되지 않고, 아이템 손실이나 완료 상태로 실행되는 경우가 발생할 수 있습니다.
 * 이러한 시나리오를 피하려면 `publish(expectedCollectors)` 오버로드를 사용하십시오.
 *
 * ```
 * flowRangeOf(1, 5)
 *     .publish { shared -> shared.filter { it % 2 == 0 } }.log("filter")
 *     .assertResult(2, 4)
 * ```
 *
 * @param transform multicasting 된 Flow 에 대한 변환 함수
 */
fun <T, R> Flow<T>.publish(transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    multicastInternal(this, { PublishSubject() }, transform)

/**
 * 하나의 collector 를 upstream source 로 공유하고, 여러 소비자에게 값을 multicasts 합니다
 *
 * **주의**
 *
 * **coroutines/[Flow]가 구현된 방식때문에, 이 기능을 보장하지 않습니다**
 *
 * [transform] 함수는 upstream과 downstream을 연결하는 시간이 보장되지 않기 때문에,
 * upstream 아이템이 수집되고 변환되지 않고, 아이템 손실이나 완료 상태로 실행되는 경우가 발생할 수 있습니다.
 * 이러한 시나리오를 피하려면 `publish(expectedCollectors)` 오버로드를 사용하십시오.
 *
 * ```
 * flowRangeOf(1, 5)
 *     .publish(2) { shared ->
 *         merge(
 *             shared.filter { it % 2 == 1 }.log("odd"),
 *             shared.filter { it % 2 == 0 }.log("even")
 *         )
 *     }
 *     .assertResult(1, 2, 3, 4, 5)
 * ```
 *
 * @param expectedCollectors upstream을 재개하기 전에 기다릴 collector의 수를 지정하여,
 *                           지정된 수의 collector가 도착하고 upstream 아이템에 대해 준비되도록 합니다
 */
fun <T, R> Flow<T>.publish(
    expectedCollectors: Int = 3,
    transform: suspend (Flow<T>) -> Flow<R>,
): Flow<R> =
    multicastInternal(
        this,
        { MulticastSubject(expectedCollectors.coerceAtLeast(1)) },
        transform
    )
