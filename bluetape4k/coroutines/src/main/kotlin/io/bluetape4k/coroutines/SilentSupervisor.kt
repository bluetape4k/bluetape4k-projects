package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 예외를 소비(suppress)하는 [SupervisorJob] 기반 코루틴 컨텍스트를 생성합니다.
 *
 * ## 동작/계약
 * - `SupervisorJob(parent)`와 no-op `CoroutineExceptionHandler`를 결합해 반환합니다.
 * - 핸들러는 예외를 기록/재전파하지 않으므로 처리되지 않은 예외가 조용히 무시될 수 있습니다.
 * - 새 `Job`/핸들러를 매 호출마다 생성해 반환합니다.
 *
 * ```kotlin
 * val ctx = SilentSupervisor()
 * // ctx[Job] != null
 * // ctx[CoroutineExceptionHandler] != null
 * ```
 *
 * @param parent 상위 Job입니다. 지정하면 취소가 상위에서 하위로 전파됩니다.
 */
@Suppress("FunctionName")
fun SilentSupervisor(parent: Job? = null): CoroutineContext =
    SupervisorJob(parent) + CoroutineExceptionHandler { _, _ -> }
