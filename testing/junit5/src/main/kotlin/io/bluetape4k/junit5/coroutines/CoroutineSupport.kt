package io.bluetape4k.junit5.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** `runSuspendIO` / `runSuspendDefault` / `runSuspendVT` 의 기본 타임아웃 (3분 — Testcontainers 컨테이너 시작 포함 CI 환경 대응) */
val DEFAULT_SUSPEND_TEST_TIMEOUT: Duration = 180.seconds

/**
 * suspend 테스트 블록을 [runBlocking]으로 실행합니다.
 *
 * ## 동작/계약
 * - `context`를 그대로 [runBlocking]에 전달해 실행 컨텍스트를 결정합니다.
 * - 수신 객체를 변경하지 않고 호출 스레드를 블로킹해 `testBody` 완료까지 대기합니다.
 * - `testBody` 예외는 감싸지지 않고 호출자에게 그대로 전파됩니다.
 * - [timeout] 초과 시 [kotlinx.coroutines.TimeoutCancellationException]을 던집니다.
 *   단, `Thread.sleep`처럼 suspension point가 없는 블로킹 코드는 타임아웃이 동작하지 않습니다.
 *
 * ```kotlin
 * var done = false
 * runSuspendTest { done = true }
 * // done == true
 * ```
 *
 * @param context [runBlocking]에 전달할 코루틴 컨텍스트
 * @param timeout 테스트 최대 허용 시간 (기본값: [DEFAULT_SUSPEND_TEST_TIMEOUT])
 * @param testBody 실행할 suspend 테스트 본문
 */
inline fun runSuspendTest(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration = DEFAULT_SUSPEND_TEST_TIMEOUT,
    crossinline testBody: suspend CoroutineScope.() -> Unit,
) {
    runBlocking(context) {
        withTimeout(timeout) { testBody(this) }
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
 * @param timeout 테스트 최대 허용 시간 (기본값: [DEFAULT_SUSPEND_TEST_TIMEOUT])
 * @param testBody 실행할 suspend 테스트 본문
 */
inline fun runSuspendIO(
    timeout: Duration = DEFAULT_SUSPEND_TEST_TIMEOUT,
    crossinline testBody: suspend CoroutineScope.() -> Unit,
) {
    runSuspendTest(Dispatchers.IO, timeout, testBody)
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
 * @param timeout 테스트 최대 허용 시간 (기본값: [DEFAULT_SUSPEND_TEST_TIMEOUT])
 * @param testBody 실행할 suspend 테스트 본문
 */
inline fun runSuspendDefault(
    timeout: Duration = DEFAULT_SUSPEND_TEST_TIMEOUT,
    crossinline testBody: suspend CoroutineScope.() -> Unit,
) {
    runSuspendTest(Dispatchers.Default, timeout, testBody)
}

/**
 * Virtual Thread Per Task Executor 기반의 [ExecutorCoroutineDispatcher]입니다.
 *
 * ## 동작/계약
 * - 최초 접근 시 한 번만 생성되며 이후 재사용됩니다.
 * - JVM 종료 시 Shutdown Hook을 통해 [ExecutorCoroutineDispatcher.close]가 자동 호출됩니다.
 * - [ExecutorCoroutineDispatcher] 타입으로 유지해 `close()` 호출 수단을 보존합니다.
 */
@PublishedApi
internal val Dispatchers.VT: ExecutorCoroutineDispatcher by lazy {
    Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher().also { dispatcher ->
        Runtime.getRuntime().addShutdownHook(Thread { dispatcher.close() })
    }
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
 * @param timeout 테스트 최대 허용 시간 (기본값: [DEFAULT_SUSPEND_TEST_TIMEOUT])
 * @param testBody 실행할 suspend 테스트 본문
 */
inline fun runSuspendVT(
    timeout: Duration = DEFAULT_SUSPEND_TEST_TIMEOUT,
    crossinline testBody: suspend CoroutineScope.() -> Unit,
) {
    runSuspendTest(Dispatchers.VT, timeout, testBody)
}
