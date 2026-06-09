package io.bluetape4k.workflow.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible

/**
 * 동기 [Work]를 코루틴 [SuspendWork]로 변환합니다.
 *
 * [Dispatchers.IO]에서 interruptible 블로킹 실행을 래핑합니다.
 *
 * ```kotlin
 * val suspendWork = blockingWork.asSuspend()
 * ```
 *
 * @return 변환된 [SuspendWork]
 */
fun Work.asSuspend(): SuspendWork = SuspendWork { ctx ->
    runInterruptible(Dispatchers.IO) { execute(ctx) }
}

/**
 * Converts a coroutine [SuspendWork] to a blocking [Work].
 *
 * Uses [runBlocking] internally. **Do not call from a coroutine context** —
 * doing so may cause deadlocks or thread starvation. Use [SuspendWork] directly
 * in coroutine contexts instead.
 *
 * ```kotlin
 * val blockingWork = suspendWork.asBlocking()
 * ```
 *
 * @return converted [Work]
 */
@Deprecated(
    message = "asBlocking() uses runBlocking and risks deadlock in coroutine contexts. Use SuspendWork directly instead.",
    level = DeprecationLevel.WARNING
)
fun SuspendWork.asBlocking(): Work = Work { ctx ->
    runBlocking { execute(ctx) }
}
