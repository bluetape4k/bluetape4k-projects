package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 예외를 상위로 전파하지 않는 [SupervisorJob] + [CoroutineExceptionHandler] 컨텍스트를 생성합니다.
 *
 * ```
 * val scope = CoroutineScope(SilentSupervisor())
 * scope.launch { error("ignored") }
 * scope.launch { println("still running") }
 * ```
 */
@Suppress("FunctionName")
fun SilentSupervisor(parent: Job? = null): CoroutineContext =
    SupervisorJob(parent) + CoroutineExceptionHandler { _, _ -> }
