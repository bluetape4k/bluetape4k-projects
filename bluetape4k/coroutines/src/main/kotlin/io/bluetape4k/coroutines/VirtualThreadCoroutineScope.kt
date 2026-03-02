package io.bluetape4k.coroutines

import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 가상 스레드 dispatcher(`Dispatchers.VT`) 기반 [CloseableCoroutineScope] 구현입니다.
 *
 * ## 동작/계약
 * - 내부 `SupervisorJob`으로 자식 코루틴 실패를 독립적으로 처리합니다.
 * - `close()`는 상위 타입 동작으로 자식 작업 취소와 컨텍스트 취소를 수행합니다.
 * - dispatcher는 가상 스레드 실행기를 사용하므로 대량 동시 대기 작업에 적합합니다.
 *
 * ```kotlin
 * val scope = VirtualThreadCoroutineScope()
 * // scope.coroutineContext[CoroutineDispatcher] == Dispatchers.VT
 * ```
 */
open class VirtualThreadCoroutineScope: CloseableCoroutineScope() {

    companion object: KLoggingChannel()

    private val job: Job = SupervisorJob()

    /**
     * 이 스코프가 사용하는 코루틴 컨텍스트입니다.
     *
     * ## 동작/계약
     * - `Dispatchers.VT + SupervisorJob` 조합을 반환합니다.
     * - 조회 시 새 인스턴스를 만들지 않고 생성 시점 컨텍스트를 반환합니다.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.VT + job

    override fun toString(): String =
        "VirtualThreadCoroutineScope(coroutineContext=$coroutineContext)"
}
