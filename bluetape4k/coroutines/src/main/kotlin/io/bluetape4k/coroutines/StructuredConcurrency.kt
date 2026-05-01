package io.bluetape4k.coroutines

import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeSupervised
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.concurrent.virtualthread.VirtualThreads
import io.bluetape4k.concurrent.virtualthread.withVirtualDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ThreadFactory

/**
 * Fail-fast `StructuredTaskScope`를 코루틴에서 사용할 수 있는 suspend 함수입니다.
 *
 * ## 동작/계약
 * - `Dispatchers.VT`(가상 스레드)에서 [StructuredTaskScopes.failFast]를 실행합니다.
 * - 하나의 subtask라도 실패하면 나머지를 즉시 취소하고 예외를 전파합니다.
 * - 코루틴 취소 시 scope 내 미완료 작업을 정리합니다.
 *
 * ```kotlin
 * val sum = taskScope {
 *     val a = fork { 1 }
 *     val b = fork { 2 }
 *     join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // sum == 3
 * ```
 *
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
 * @param block scope 실행 블록 — `this`가 [StructuredTaskScopeAll]
 * @return [block]의 실행 결과
 * @see StructuredTaskScopes.failFast
 */
suspend fun <T> taskScope(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory(),
    block: StructuredTaskScopeAll.() -> T,
): T = withVirtualDispatcher {
    StructuredTaskScopes.failFast(name, factory) { scope -> block(scope) }
}

/**
 * Fail-fast `StructuredTaskScope`를 코루틴에서 사용할 수 있는 suspend 함수입니다.
 *
 * [taskScope]의 이름이 의도를 더 명확히 표현하는 별칭입니다.
 *
 * ```kotlin
 * val sum = failFastTaskScope {
 *     val a = fork { 1 }
 *     val b = fork { 2 }
 *     join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // sum == 3
 * ```
 *
 * @see taskScope
 */
suspend fun <T> failFastTaskScope(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory(),
    block: StructuredTaskScopeAll.() -> T,
): T = taskScope(name, factory, block)

/**
 * First-success `StructuredTaskScope`를 코루틴에서 사용할 수 있는 suspend 함수입니다.
 *
 * ## 동작/계약
 * - `Dispatchers.VT`(가상 스레드)에서 [StructuredTaskScopes.firstSuccess]를 실행합니다.
 * - 첫 번째 성공한 subtask 결과를 반환하고 나머지를 취소합니다.
 * - 모든 subtask가 실패하면 [StructuredTaskScopeAny.result]의 mapper 예외가 발생합니다.
 *
 * ```kotlin
 * val winner = firstSuccessTaskScope<String> {
 *     fork { fetchFromPrimary() }
 *     fork { fetchFromFallback() }
 *     join().result { IllegalStateException("모든 소스 실패: ${it.message}") }
 * }
 * ```
 *
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
 * @param block scope 실행 블록 — `this`가 [StructuredTaskScopeAny]<T>
 * @return 가장 먼저 성공한 subtask의 결과
 * @see StructuredTaskScopes.firstSuccess
 */
suspend fun <T> firstSuccessTaskScope(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory(),
    block: StructuredTaskScopeAny<T>.() -> T,
): T = withVirtualDispatcher {
    StructuredTaskScopes.firstSuccess(name, factory) { scope -> block(scope) }
}

/**
 * Supervised `StructuredTaskScope`를 코루틴에서 사용할 수 있는 suspend 함수입니다.
 *
 * ## 동작/계약
 * - `Dispatchers.VT`(가상 스레드)에서 [StructuredTaskScopes.supervised]를 실행합니다.
 * - 하나의 subtask가 실패해도 나머지 subtask를 계속 실행합니다 (fail-fast 아님).
 * - [join] 이후 [results]로 `Result<T>` 통합 조회하거나, [successfulResults] / [failedExceptions]로 분리합니다.
 *
 * ```kotlin
 * val allResults = supervisedTaskScope<Int, List<Result<Int>>> {
 *     fork { 1 }
 *     fork { throw RuntimeException("실패") }
 *     fork { 3 }
 *     join()
 *     results()
 * }
 * // allResults.filter { it.isSuccess }.map { it.getOrThrow() } == [1, 3]
 * ```
 *
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
 * @param block scope 실행 블록 — `this`가 [StructuredTaskScopeSupervised]<T>
 * @return [block]의 실행 결과
 * @see StructuredTaskScopes.supervised
 */
suspend fun <T, R> supervisedTaskScope(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory(),
    block: StructuredTaskScopeSupervised<T>.() -> R,
): R = withVirtualDispatcher {
    StructuredTaskScopes.supervised(name, factory) { scope -> block(scope) }
}

/**
 * Fail-fast `StructuredTaskScope`를 비동기 [Deferred]로 실행합니다.
 *
 * ## 동작/계약
 * - `Dispatchers.VT`(가상 스레드)에서 [StructuredTaskScopes.failFast]를 비동기 실행합니다.
 * - 즉시 [Deferred]를 반환하고, 실제 실행은 백그라운드에서 진행됩니다.
 * - [Deferred.await] 시 결과 또는 예외를 받습니다.
 * - 코루틴 취소 시 scope 내 미완료 작업을 정리합니다.
 *
 * ```kotlin
 * // 여러 StructuredTaskScope를 병렬로 실행
 * val deferred1 = asyncTaskScope {
 *     val a = fork { fetchData("source1") }
 *     join().throwIfFailed()
 *     a.get()
 * }
 * val deferred2 = asyncTaskScope {
 *     val b = fork { fetchData("source2") }
 *     join().throwIfFailed()
 *     b.get()
 * }
 * val (result1, result2) = awaitAll(deferred1, deferred2)
 * ```
 *
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
 * @param block scope 실행 블록 — `this`가 [StructuredTaskScopeAll]
 * @return 결과를 나타내는 [Deferred]
 * @see taskScope
 * @see StructuredTaskScopes.failFast
 */
fun <T> CoroutineScope.asyncTaskScope(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory(),
    block: StructuredTaskScopeAll.() -> T,
): Deferred<T> = async(Dispatchers.VT) {
    StructuredTaskScopes.failFast(name, factory) { scope -> block(scope) }
}

/**
 * Supervised `StructuredTaskScope`를 비동기 [Deferred]로 실행합니다.
 *
 * ## 동작/계약
 * - `Dispatchers.VT`(가상 스레드)에서 [StructuredTaskScopes.supervised]를 비동기 실행합니다.
 * - 즉시 [Deferred]를 반환하고, 실제 실행은 백그라운드에서 진행됩니다.
 * - 하나의 subtask가 실패해도 나머지를 계속 실행합니다 (fail-fast 아님).
 * - [Deferred.await] 시 최종 결과(성공/실패 혼합 가능)를 받습니다.
 *
 * ```kotlin
 * val deferred = asyncSupervisedTaskScope<Int, List<Result<Int>>> {
 *     fork { 1 }
 *     fork { throw RuntimeException("실패") }
 *     fork { 3 }
 *     join()
 *     results()
 * }
 * val allResults = deferred.await()
 * // allResults.filter { it.isSuccess }.size == 2
 * ```
 *
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
 * @param block scope 실행 블록 — `this`가 [StructuredTaskScopeSupervised]<T>
 * @return 결과를 나타내는 [Deferred]
 * @see supervisedTaskScope
 * @see StructuredTaskScopes.supervised
 */
fun <T, R> CoroutineScope.asyncSupervisedTaskScope(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory(),
    block: StructuredTaskScopeSupervised<T>.() -> R,
): Deferred<R> = async(Dispatchers.VT) {
    StructuredTaskScopes.supervised(name, factory) { scope -> block(scope) }
}
