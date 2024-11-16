package io.bluetape4k.concurrent.virtualthread

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Virtual Thread 를 이용하여 Coroutine 작업을 Blocking 방식으로 수행합니다.
 *
 * ```
 * val result = runVirtualBlocking {
 *      // Virtual Thread 를 Coroutines Context 로 사용합니다.
 *      delay(1000)
 *      log.debug { "Job is done" }
 *      42
 * }
 * println(result) // 42
 * ```
 *
 * @param T
 * @param context
 * @param block
 * @receiver
 * @return
 */
fun <T> runVirtualBlocking(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = runBlocking(context + Dispatchers.VT, block)

/**
 * Virtual Thread 를 이용하여 Coroutine 작업을 Non-Blocking 방식으로 수행합니다.
 *
 * ```
 * val task = async {
 *    withVirtualContext {
 *      // Virtual Thread 에서 동기/비동기 코드를 실행됩니다.
 *      Thread.sleep(1000)
 *      log.debug { "Job $it is done" }
 *      42
 *    }
 * }
 *
 * task.await() // 42
 * ```
 *
 * @param T
 * @param context
 * @param block
 * @receiver
 * @return
 */
suspend fun <T> withVirtualContext(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = withContext(context + Dispatchers.VT, block)
