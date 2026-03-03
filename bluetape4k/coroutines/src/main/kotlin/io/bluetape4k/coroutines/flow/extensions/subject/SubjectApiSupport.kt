package io.bluetape4k.coroutines.flow.extensions.subject

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Subject에 collector가 최소 1개 등록될 때까지 대기합니다.
 *
 * ## 동작/계약
 * - `hasCollectors`가 `true`가 될 때까지 1ms 간격으로 반복 확인합니다.
 * - [timeout] 내에 collector가 등록되지 않으면 `TimeoutCancellationException`을 던집니다.
 * - 수신 객체를 변경하지 않으며 추가 객체 할당 없이 상태 조회만 반복합니다.
 *
 * ```kotlin
 * val subject = PublishSubject<Int>()
 * launch { subject.collect {} }
 * subject.awaitCollector()
 * // subject.hasCollectors == true
 * ```
 *
 * @param timeout 대기 최대 시간입니다. 기본값은 5초이며 초과 시 `TimeoutCancellationException`이 발생합니다.
 */
suspend fun <T> SubjectApi<T>.awaitCollector(timeout: Duration = 5.seconds) {
    withTimeout(timeout) {
        while (!hasCollectors) {
            delay(1)
        }
    }
}

/**
 * Subject에 지정한 수 이상의 collector가 등록될 때까지 대기합니다.
 *
 * ## 동작/계약
 * - `minCollectorCount`가 1 미만이면 내부에서 1로 보정합니다.
 * - `collectorCount`가 목표 이상이 될 때까지 1ms 간격으로 반복 확인합니다.
 * - [timeout] 내에 목표 수에 도달하지 않으면 `TimeoutCancellationException`을 던집니다.
 *
 * ```kotlin
 * val subject = PublishSubject<Int>()
 * launch { subject.collect {} }
 * launch { subject.collect {} }
 * subject.awaitCollectors(2)
 * // subject.collectorCount >= 2
 * ```
 *
 * @param minCollectorCount 기다릴 최소 collector 수입니다. 1 미만 값은 1로 보정됩니다.
 * @param timeout 대기 최대 시간입니다. 기본값은 5초이며 초과 시 `TimeoutCancellationException`이 발생합니다.
 */
suspend fun <T> SubjectApi<T>.awaitCollectors(minCollectorCount: Int = 1, timeout: Duration = 5.seconds) {
    val limit = minCollectorCount.coerceAtLeast(1)
    withTimeout(timeout) {
        while (collectorCount < limit) {
            delay(1)
        }
    }
}

/**
 * 두 값을 null-safe하게 동등 비교합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `(left as Any?) == (right as Any?)`를 수행합니다.
 * - `left` 또는 `right`가 `null`이어도 안전하게 비교합니다.
 * - 비교 자체는 수신 객체를 변경하지 않고 추가 컬렉션 할당 없이 실행됩니다.
 *
 * ```kotlin
 * val same = areEqualAsAny(10, 10)
 * // same == true
 * val nullSame = areEqualAsAny(null, null)
 * // nullSame == true
 * ```
 *
 * @param left 비교할 왼쪽 값입니다.
 * @param right 비교할 오른쪽 값입니다.
 */
fun <L, R> areEqualAsAny(left: L, right: R): Boolean =
    (left as Any?) == (right as Any?)
