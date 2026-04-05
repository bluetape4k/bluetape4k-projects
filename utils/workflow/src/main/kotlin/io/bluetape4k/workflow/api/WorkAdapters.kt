package io.bluetape4k.workflow.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 동기 [Work]를 코루틴 [SuspendWork]로 변환합니다.
 *
 * [Dispatchers.IO]에서 블로킹 실행을 래핑합니다.
 *
 * ```kotlin
 * val suspendWork = blockingWork.asSuspend()
 * ```
 *
 * @return 변환된 [SuspendWork]
 */
fun Work.asSuspend(): SuspendWork = SuspendWork { ctx ->
    withContext(Dispatchers.IO) { execute(ctx) }
}

/**
 * 코루틴 [SuspendWork]를 동기 [Work]로 변환합니다.
 *
 * 내부에서 [runBlocking]을 사용하므로 코루틴 컨텍스트 내에서는 사용하지 마세요.
 *
 * ```kotlin
 * val blockingWork = suspendWork.asBlocking()
 * ```
 *
 * @return 변환된 [Work]
 */
fun SuspendWork.asBlocking(): Work = Work { ctx ->
    runBlocking { execute(ctx) }
}
