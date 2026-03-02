package io.bluetape4k.coroutines.flow.extensions.subject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * Flow 기반 Subject가 제공해야 하는 공통 계약입니다.
 *
 * ## 동작/계약
 * - `emit`으로 전달된 요소를 현재 등록된 collector로 전달하는 역할을 가지며, 전달 방식은 구현체마다 다를 수 있습니다.
 * - `hasCollectors`와 `collectorCount`는 조회 시점의 collector 상태를 나타내며 동시 접근 중에는 즉시 값이 바뀔 수 있습니다.
 * - 오류 종료(`emitError`)와 정상 종료(`complete`) 이후의 collect/emit 처리 방식은 구현체 문서를 따릅니다.
 *
 * ```kotlin
 * val subject: SubjectApi<Int> = PublishSubject()
 * // subject.collectorCount == 0
 * // subject.hasCollectors == false
 * ```
 */
interface SubjectApi<T>: FlowCollector<T>, Flow<T> {

    /**
     * 현재 등록된 collector가 1개 이상인지 반환합니다.
     *
     * ## 동작/계약
     * - `true`면 최소 1개의 collector가 활성 상태이고, `false`면 등록된 collector가 없습니다.
     * - 값은 스냅샷이며, 다른 코루틴의 collect 시작/종료에 따라 즉시 변할 수 있습니다.
     */
    val hasCollectors: Boolean

    /**
     * 현재 등록된 collector 개수를 반환합니다.
     *
     * ## 동작/계약
     * - 반환값은 조회 시점의 collector 수입니다.
     * - 동시 collect/취소가 발생하면 다음 조회에서 값이 달라질 수 있습니다.
     */
    val collectorCount: Int

    /**
     * Subject를 오류 상태로 종료합니다.
     *
     * ## 동작/계약
     * - 구현체는 전달받은 예외를 현재 collector에게 전달하거나 내부 종료 상태로 기록합니다.
     * - `ex`가 `null`일 때의 처리(무시/정상 종료 대체/예외 전달)는 구현체별로 다를 수 있습니다.
     *
     * ```kotlin
     * val subject = PublishSubject<Int>()
     * subject.emitError(IllegalStateException("boom"))
     * // 이후 collect 시 IllegalStateException 전파 가능
     * ```
     *
     * @param ex 종료 원인 예외입니다. `null` 처리 방식은 구현체 문서를 따릅니다.
     */
    suspend fun emitError(ex: Throwable?)

    /**
     * Subject를 정상 종료합니다.
     *
     * ## 동작/계약
     * - 구현체는 현재 collector에 완료 신호를 전달하고 이후 신규 collect 처리 정책을 적용합니다.
     * - 종료 이후 동작(요소 재방출, 즉시 완료, 예외 전파 등)은 구현체별 계약을 따릅니다.
     *
     * ```kotlin
     * val subject = PublishSubject<Int>()
     * subject.complete()
     * // 이후 신규 collector는 즉시 완료될 수 있음
     * ```
     */
    suspend fun complete()
}
