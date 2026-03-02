package io.bluetape4k.junit5.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * suspend 테스트 블록을 [runBlocking]으로 실행합니다.
 *
 * ## 동작/계약
 * - `context`를 그대로 [runBlocking]에 전달해 실행 컨텍스트를 결정합니다.
 * - 수신 객체를 변경하지 않고 호출 스레드를 블로킹해 `testBody` 완료까지 대기합니다.
 * - `testBody` 예외는 감싸지지 않고 호출자에게 그대로 전파됩니다.
 *
 * ```kotlin
 * var done = false
 * runSuspendTest { done = true }
 * // done == true
 * ```
 *
 * @param context [runBlocking]에 전달할 코루틴 컨텍스트
 * @param testBody 실행할 suspend 테스트 본문
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
 * [Dispatchers.IO] 컨텍스트에서 suspend 테스트 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `runSuspendTest(Dispatchers.IO, ...)`를 호출합니다.
 * - 별도 할당 없이 wrapper 호출만 추가합니다.
 *
 * ```kotlin
 * val threadNames = mutableListOf<String>()
 * runSuspendIO { threadNames += Thread.currentThread().name }
 * // threadNames.isNotEmpty() == true
 * ```
 *
 * @param testBody 실행할 suspend 테스트 본문
 */
inline fun runSuspendIO(crossinline testBody: suspend CoroutineScope.() -> Unit) {
    runSuspendTest(Dispatchers.IO, testBody)
}

/**
 * [Dispatchers.Default] 컨텍스트에서 suspend 테스트 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `runSuspendTest(Dispatchers.Default, ...)`를 호출합니다.
 * - 호출자 상태를 변경하지 않고 테스트 블록 완료까지 동기 대기합니다.
 *
 * ```kotlin
 * var sum = 0
 * runSuspendDefault { sum = (1..3).sum() }
 * // sum == 6
 * ```
 *
 * @param testBody 실행할 suspend 테스트 본문
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
 * virtual thread dispatcher에서 suspend 테스트 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `runSuspendTest(Dispatchers.VT, ...)`를 호출합니다.
 * - `Dispatchers.VT`는 최초 접근 시 virtual-thread executor를 한 번 생성합니다.
 * - 테스트 블록 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * var executed = false
 * runSuspendVT { executed = true }
 * // executed == true
 * ```
 *
 * @param testBody 실행할 suspend 테스트 본문
 */
inline fun runSuspendVT(crossinline testBody: suspend CoroutineScope.() -> Unit) {
    runSuspendTest(Dispatchers.VT, testBody)
}
