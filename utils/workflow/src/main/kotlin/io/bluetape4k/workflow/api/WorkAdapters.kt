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
 * coroutine [SuspendWork]를 blocking [Work]로 변환합니다.
 *
 * 내부적으로 [runBlocking]을 사용합니다. **coroutine context에서는 호출하지 마십시오.**
 * 그렇게 하면 deadlock 또는 thread starvation이 발생할 수 있습니다. coroutine context에서는
 * [SuspendWork]를 직접 사용하십시오.
 *
 * ```kotlin
 * val blockingWork = suspendWork.asBlocking()
 * ```
 *
 * @return 변환된 [Work]입니다.
 */
@Deprecated(
    message = "asBlocking() uses runBlocking and risks deadlock in coroutine contexts. Use SuspendWork directly instead.",
    level = DeprecationLevel.WARNING
)
fun SuspendWork.asBlocking(): Work = Work { ctx ->
    runBlocking { execute(ctx) }
}
