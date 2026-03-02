package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * `Dispatchers.IO` 기반의 [CloseableCoroutineScope] 구현입니다.
 *
 * ## 동작/계약
 * - 내부에 `SupervisorJob`을 사용해 자식 실패를 독립적으로 관리합니다.
 * - `close()` 호출 시 [clearJobs]를 통해 자식 작업과 컨텍스트 취소를 수행합니다.
 * - I/O dispatcher를 사용하므로 블로킹 I/O 래핑 작업 실행에 적합합니다.
 *
 * ```kotlin
 * val scope = IoCoroutineScope()
 * // scope.coroutineContext[CoroutineDispatcher] == Dispatchers.IO
 * ```
 */
open class IoCoroutineScope: CloseableCoroutineScope() {

    companion object: KLoggingChannel()

    private val job: Job = SupervisorJob()

    /**
     * 이 스코프가 사용하는 코루틴 컨텍스트입니다.
     *
     * ## 동작/계약
     * - `Dispatchers.IO + SupervisorJob` 조합을 반환합니다.
     * - 조회 시 동일 컨텍스트 인스턴스를 반환하며 상태를 변경하지 않습니다.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    /**
     * 스코프를 닫고 실행 중 작업을 취소합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [clearJobs]를 호출합니다.
     * - 중복 호출 시 [CloseableCoroutineScope]의 원자 플래그에 의해 한 번만 실제 취소됩니다.
     */
    override fun close() {
        clearJobs()
    }

    override fun toString(): String =
        "IoCoroutineScope(coroutineContext=$coroutineContext)"
}
