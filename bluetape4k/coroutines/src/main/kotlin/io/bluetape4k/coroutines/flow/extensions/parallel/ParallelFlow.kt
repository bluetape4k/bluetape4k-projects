package io.bluetape4k.coroutines.flow.extensions.parallel

import kotlinx.coroutines.flow.FlowCollector

/**
 * 여러 rail(병렬 레인)로 분산된 Flow 수집 계약입니다.
 *
 * ## 동작/계약
 * - [parallelism] 개수만큼 collector를 제공해 각 rail을 동시에 수집합니다.
 * - 구현체는 collector 개수 일치 여부를 검증하고 불일치 시 예외를 던질 수 있습니다.
 * - rail 간 순서는 보장되지 않으며, 순차 재조합이 필요하면 별도 연산이 필요합니다.
 *
 * ```kotlin
 * val p: ParallelFlow<Int> = /* ... */
 * // p.parallelism == collector 개수
 * ```
 */
interface ParallelFlow<out T> {

    /**
     * 이 ParallelFlow의 레인 수를 반환합니다.
     */
    val parallelism: Int

    /**
     * 각 rail을 대응 collector로 수집합니다.
     *
     * ## 동작/계약
     * - `collectors.size == parallelism`을 기대합니다.
     * - collector 수 불일치나 수집 중 예외 처리 방식은 구현체 계약을 따릅니다.
     *
     * @param collectors 각 rail을 수집할 collector 배열입니다.
     */
    suspend fun collect(vararg collectors: FlowCollector<T>)
}
