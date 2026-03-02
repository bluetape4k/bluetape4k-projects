package io.bluetape4k.coroutines.flow

import kotlinx.coroutines.flow.FlowCollector

/**
 * 전달된 값을 무시하는 no-op [FlowCollector]입니다.
 *
 * ## 동작/계약
 * - `emit` 호출 시 값을 소비하지 않고 즉시 반환합니다.
 * - 내부 상태를 가지지 않는 singleton 객체이며 스레드 안전하게 재사용 가능합니다.
 * - 값 저장이나 추가 할당이 없어 부하 테스트/드레인 용도로 사용할 수 있습니다.
 *
 * ```kotlin
 * NoopCollector.emit(1)
 * NoopCollector.emit("x")
 * // 부수효과 없이 종료됨
 * ```
 */
object NoopCollector: FlowCollector<Any?> {

    /**
     * 값을 무시하고 아무 작업도 수행하지 않습니다.
     *
     * ## 동작/계약
     * - 입력값이 `null`이든 아니든 동일하게 무시합니다.
     * - 예외를 발생시키지 않고 즉시 반환합니다.
     *
     * @param value 무시할 값입니다.
     */
    override suspend fun emit(value: Any?) {
        // Nothing to do
    }
}
