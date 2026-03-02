package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.extensions.subject.MulticastSubject
import io.bluetape4k.coroutines.flow.extensions.subject.PublishSubject
import kotlinx.coroutines.flow.Flow

/**
 * source를 [PublishSubject]로 공유한 뒤 selector 결과 Flow를 반환합니다.
 *
 * ## 동작/계약
 * - selector는 같은 source를 공유하는 hot subject를 입력으로 받습니다.
 * - PublishSubject 특성상 구독 이전 과거 값은 재생되지 않습니다.
 * - 내부 실행 모델은 [multicastInternal]에 위임됩니다.
 *
 * ```kotlin
 * val shared = source.publish { shared -> shared.take(2) }
 * // shared는 source를 단일 구독으로 공유한다.
 * ```
 *
 * @param transform 공유된 Flow를 받아 최종 결과 Flow를 만드는 selector입니다.
 */
fun <T, R> Flow<T>.publish(transform: suspend (Flow<T>) -> Flow<R>): Flow<R> =
    multicastInternal(this, { PublishSubject() }, transform)

/**
 * source를 [MulticastSubject]로 공유하고 기대 collector 수를 지정합니다.
 *
 * ## 동작/계약
 * - `expectedCollectors`는 1 미만이면 1로 보정됩니다.
 * - 보정된 수만큼 collector가 준비될 때까지 subject가 producer를 대기시킬 수 있습니다.
 * - 내부 실행은 [multicastInternal]에 위임됩니다.
 *
 * ```kotlin
 * val shared = source.publish(expectedCollectors = 2) { shared -> shared }
 * // 두 collector가 준비될 때까지 emit이 대기할 수 있다.
 * ```
 *
 * @param expectedCollectors producer 진행 전 대기할 최소 collector 수입니다.
 * @param transform 공유된 Flow를 받아 최종 결과 Flow를 만드는 selector입니다.
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
