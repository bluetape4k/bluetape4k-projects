package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 예외가 발생하더라도, 무시하는 SupervisorJob을 생성합니다.
 *
 * ```
 * launch(SilentSupervisor()) {
 *    throw Bluetape4kException("Error")
 *    // 예외가 발생해도, 무시됩니다.
 *    // catch 블록이 없어도, 코루틴이 종료되지 않습니다.
 *    // 코루틴이 종료되지 않으면, 부모 코루틴도 종료되지 않습니다.
 *    // 부모 코루틴이 종료되지 않으면, 자식 코루틴도 종료되지 않습니다.
 *    // 따라서, 부모 코루틴이 종료될 때까지, 자식 코루틴이 계속 실행됩니다.
 *    // 이러한 특성을 이용해서, 코루틴을 계속 실행하고 싶을 때 사용합니다.
 *    // 예를 들어, 서버에서 데이터를 주기적으로 가져와서 처리하는 작업을 할 때 사용합니다.
 * }
 * ```
 */
@Suppress("FunctionName")
fun SilentSupervisor(parent: Job? = null): CoroutineContext =
    SupervisorJob(parent) + CoroutineExceptionHandler { _, _ -> }
