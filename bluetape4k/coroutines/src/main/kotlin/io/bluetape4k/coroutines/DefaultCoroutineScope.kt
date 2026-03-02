package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * `Dispatchers.Default` 기반의 기본 [CloseableCoroutineScope] 구현입니다.
 *
 * ## 동작/계약
 * - 내부에 `SupervisorJob`을 사용하여 형제 코루틴 실패가 전체 스코프 취소로 즉시 전파되지 않습니다.
 * - `close()`는 상위 타입 구현을 통해 자식 작업과 컨텍스트를 취소합니다.
 * - 스코프 생성 시 dispatcher/job 조합을 1회 할당하고 이후 동일 컨텍스트를 재사용합니다.
 *
 * ```kotlin
 * val scope = DefaultCoroutineScope()
 * // scope.coroutineContext[Job] != null
 * // scope.coroutineContext[CoroutineDispatcher] == Dispatchers.Default
 * ```
 */
open class DefaultCoroutineScope: CloseableCoroutineScope() {

    companion object: KLoggingChannel()

    private val job: CompletableJob = SupervisorJob()

    /**
     * 이 스코프가 사용하는 코루틴 컨텍스트입니다.
     *
     * ## 동작/계약
     * - `Dispatchers.Default + SupervisorJob` 조합을 반환합니다.
     * - 조회 시 새 컨텍스트를 만들지 않고 생성 시점 인스턴스를 그대로 반환합니다.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    override fun toString(): String =
        "DefaultCoroutineScope(coroutineContext=$coroutineContext)"
}
