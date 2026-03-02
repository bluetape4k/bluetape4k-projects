package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * 값을 방출하거나 완료하지 않고 취소될 때까지 대기하는 Flow 타입입니다.
 *
 * ## 동작/계약
 * - `collect`는 `awaitCancellation()`로 진입해 무한 대기합니다.
 * - 정상 완료/오류 방출 없이 외부 취소에 의해서만 종료됩니다.
 * - singleton 구현([NeverFlow])을 재사용하므로 추가 할당이 없습니다.
 *
 * ```kotlin
 * val f = neverFlow()
 * // f.collect { ... } 는 취소 전까지 반환되지 않는다.
 * ```
 */
sealed interface NeverFlow: Flow<Nothing> {

    companion object: NeverFlow {
        override suspend fun collect(collector: FlowCollector<Nothing>) = awaitCancellation()
    }

    /**
     * 취소될 때까지 대기합니다.
     *
     * ## 동작/계약
     * - 값을 emit하지 않습니다.
     * - 완료 신호를 보내지 않습니다.
     * - 외부에서 코루틴이 취소되면 `CancellationException`으로 종료됩니다.
     *
     * @param collector 사용되지 않지만 [Flow] 시그니처를 위해 필요합니다.
     */
    override suspend fun collect(collector: FlowCollector<Nothing>)
}

/**
 * singleton [NeverFlow] 인스턴스를 반환합니다.
 *
 * ## 동작/계약
 * - 항상 동일 인스턴스를 반환합니다.
 * - collect 동작은 [NeverFlow] 계약과 동일하게 취소 전까지 대기합니다.
 */
fun neverFlow(): NeverFlow = NeverFlow
