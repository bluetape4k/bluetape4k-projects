package io.bluetape4k.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

private object SilentSupervisorLog: KLogging()

/**
 * 예외를 로그에 기록하고 전파하지 않는 [SupervisorJob] 기반 코루틴 컨텍스트를 생성합니다.
 *
 * ## 동작/계약
 * - `SupervisorJob(parent)`와 예외 로깅 `CoroutineExceptionHandler`를 결합해 반환합니다.
 * - 처리되지 않은 예외는 WARN 레벨로 로깅되며 재전파하지 않습니다.
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
    SupervisorJob(parent) + CoroutineExceptionHandler { _, throwable ->
        SilentSupervisorLog.log.warn(throwable) { "SilentSupervisor: 처리되지 않은 코루틴 예외를 무시합니다." }
    }
