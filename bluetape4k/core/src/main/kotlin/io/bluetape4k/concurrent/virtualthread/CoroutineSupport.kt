package io.bluetape4k.concurrent.virtualthread

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Virtual threads 를 사용하는 Dispatcher([Dispatchers.VT])를 이용하여 Coroutine 작업을 Blocking 방식으로 수행합니다.
 *
 * ```kotlin
 * val result = runVirtualBlocking {
 *      // Virtual Thread 를 Coroutines Context 로 사용합니다.
 *      delay(1000)
 *      log.debug { "Job is done" }
 *      42
 * }
 * // result == 42
 * ```
 *
 * @param T 반환할 타입
 * @param context 추가 코루틴 컨텍스트 (기본값: [EmptyCoroutineContext])
 * @param block Virtual Thread Dispatcher 위에서 실행할 suspend 블록
 * @return [block]의 실행 결과
 */
fun <T> runVirtualBlocking(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = runBlocking(context + Dispatchers.VT, block)

/**
 * Virtual Thread Dispatcher([Dispatchers.VT])에서 코루틴 블록을 실행합니다.
 *
 * ```kotlin
 * val result = runBlocking {
 *     withVirtualDispatcher {
 *         Thread.sleep(1000)
 *         42
 *     }
 * }
 * // result == 42
 * ```
 *
 * @param T 반환할 타입
 * @param context 추가 코루틴 컨텍스트 (기본값: [EmptyCoroutineContext])
 * @param block [Dispatchers.VT] 위에서 실행할 suspend 블록
 * @return [block]의 실행 결과
 */
suspend fun <T> withVirtualDispatcher(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = withContext(context + Dispatchers.VT, block)

/**
 * IO Dispatcher([Dispatchers.IO])에서 코루틴 블록을 실행합니다.
 *
 * ```kotlin
 * val content = withIoDispatcher {
 *     File("data.txt").readText()
 * }
 * ```
 *
 * @param T 반환할 타입
 * @param context 추가 코루틴 컨텍스트 (기본값: [EmptyCoroutineContext])
 * @param block [Dispatchers.IO] 위에서 실행할 suspend 블록
 * @return [block]의 실행 결과
 */
suspend fun <T> withIoDispatcher(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = withContext(context + Dispatchers.IO, block)

/**
 * Default Dispatcher([Dispatchers.Default])에서 코루틴 블록을 실행합니다.
 *
 * ```kotlin
 * val sorted = withDefaultDispatcher {
 *     largeList.sortedBy { it.score }
 * }
 * ```
 *
 * @param T 반환할 타입
 * @param context 추가 코루틴 컨텍스트 (기본값: [EmptyCoroutineContext])
 * @param block [Dispatchers.Default] 위에서 실행할 suspend 블록
 * @return [block]의 실행 결과
 */
suspend fun <T> withDefaultDispatcher(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = withContext(context + Dispatchers.Default, block)

/**
 * Virtual threads 를 사용하는 Dispatcher([Dispatchers.VT])를 이용하여 Coroutine 작업을 Non-Blocking 방식으로 수행합니다.
 *
 * @see withVirtualDispatcher
 */
@Deprecated(
    message = "withVirtualDispatcher 로 교체되었습니다.",
    replaceWith = ReplaceWith("withVirtualDispatcher(context, block)"),
    level = DeprecationLevel.WARNING,
)
suspend fun <T> withVirtualContext(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T,
): T = withVirtualDispatcher(context, block)
