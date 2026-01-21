package io.bluetape4k.junit5.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 테스트 Block을 Blocking 해야 하는데 매번 반환 수형을 지정하는 것이 번거롭다.
 * 이 함수를 사용하면 테스트 코드의 반환값에 상관없이 사용할 수 있다.
 * 즉 `runBlocking<Unit> { ... }` 대신 `runSuspendTest { ... }` 으로 사용할 수 있다.
 *
 * ```
 * @Test
 * fun test() = runSuspendTest {
 *   // ...
 * }
 * ```
 *
 * @param context [CoroutineContext] 인스턴스 (기본: [StandardTestDispatcher])
 * @param testBody  테스트 할 코드
 */
inline fun runSuspendTest(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline testBody: suspend CoroutineScope.() -> Unit,
) {
    runBlocking(context) {
        testBody(this)
    }
}


/**
 * [Dispatchers.IO] 환경에서 [testBody]를 실행합니다.
 *
 * ```
 * @Test
 * fun test() = runSuspendIO {
 *   // ...
 * }
 * ```
 *
 * @param testBody  테스트 할 코드
 */
inline fun runSuspendIO(crossinline testBody: suspend CoroutineScope.() -> Unit) {
    runSuspendTest(Dispatchers.IO, testBody)
}

/**
 * [Dispatchers.Default] 환경에서 [testBody]를 실행합니다.
 *
 * ```
 * @Test
 * fun test() = runSuspendDefault {
 *   // ...
 * }
 * ```
 *
 * @param testBody  테스트 할 코드
 */
inline fun runSuspendDefault(crossinline testBody: suspend CoroutineScope.() -> Unit) {
    runSuspendTest(Dispatchers.Default, testBody)
}

/**
 * Virtual Thread Per TaskExecutor를 Dispatcher로 만든 것
 */
@PublishedApi
internal val Dispatchers.VT: CoroutineContext by lazy {
    Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
}

/**
 * VirtualThread Per TaskExecutor 를 Dispatcher로 [testBody]를 실행합니다.
 *
 * ```
 * @Test
 * fun test() = runSuspendVT {
 *   // virtual threads 를 Dispatchers로 사용하는 CoroutineScope에서 실행
 * }
 * ```
 *
 * @param testBody  테스트 할 코드
 * @see runSuspendIO
 */
inline fun runSuspendVT(crossinline testBody: suspend CoroutineScope.() -> Unit) {
    runSuspendTest(Dispatchers.VT, testBody)
}
