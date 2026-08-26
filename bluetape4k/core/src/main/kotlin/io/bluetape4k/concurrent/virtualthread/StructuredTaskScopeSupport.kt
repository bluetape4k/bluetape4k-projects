package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeFailFast
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeFirstSuccess
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeSupervised
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopes
import io.bluetape4k.concurrent.virtualthread.api.VirtualThreads
import java.util.concurrent.ThreadFactory

/**
 * 실패 전파형(fail-fast) 구조화된 동시성 블록을 실행합니다.
 * 하나의 subtask라도 실패하면 나머지를 즉시 중단하고 예외를 전파합니다.
 *
 * ```kotlin
 * data class Result(val a: Int, val b: String)
 *
 * val result = structuredTaskScopeFailFast { scope ->
 *     val subtask1 = scope.fork { Thread.sleep(100); 42 }
 *     val subtask2 = scope.fork { Thread.sleep(200); "hello" }
 *     scope.join().throwIfFailed()
 *     Result(subtask1.get(), subtask2.get())
 * }
 * // result == Result(a=42, b="hello")
 * ```
 *
 * @param T 반환할 타입
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory Virtual Thread 팩토리 (기본값: `VirtualThreads.threadFactory("sts-failfast-")`)
 * @param block scope를 인자로 받아 서브 작업을 fork하고 결과를 반환하는 블록
 * @return [block]의 실행 결과
 * @throws Exception 서브 작업 중 하나라도 실패하면 해당 예외를 던진다
 */
fun <T> structuredTaskScopeFailFast(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-failfast-"),
    block: (scope: StructuredTaskScopeFailFast) -> T,
): T = StructuredTaskScopes.failFast(name, factory, block)

/**
 * 성공 우선형(first-success) 구조화된 동시성 블록을 실행합니다.
 * 첫 번째 성공한 subtask 결과를 반환하고 나머지를 취소합니다.
 *
 * ```kotlin
 * val result = structuredTaskScopeFirstSuccess<String> { scope ->
 *     scope.fork { Thread.sleep(100); "result1" }
 *     scope.fork { Thread.sleep(200); "result2" }
 *     scope.join().result { IllegalStateException("all failed: ${it.message}") }
 * }
 * // 먼저 완료되는 작업의 결과를 반환한다.
 * // result == "result1"
 * ```
 *
 * @param T 반환할 타입
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory Virtual Thread 팩토리 (기본값: `VirtualThreads.threadFactory("sts-first-")`)
 * @param block scope를 인자로 받아 서브 작업을 fork하고 첫 번째 성공 결과를 반환하는 블록
 * @return 가장 먼저 성공한 서브 작업의 결과
 */
fun <T> structuredTaskScopeFirstSuccess(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-first-"),
    block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
): T = StructuredTaskScopes.firstSuccess(name, factory, block)

/**
 * 부분 실패를 허용하는(supervised) 구조화된 동시성 블록을 실행합니다.
 * 모든 subtask가 완료될 때까지 기다리며, 성공/실패 결과를 별도로 수집합니다.
 *
 * ```kotlin
 * val (results, errors) = structuredTaskScopeSupervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
 *     scope.fork { 1 }
 *     scope.fork { throw RuntimeException("fail") }
 *     scope.fork { 3 }
 *     scope.join()
 *     scope.successfulResults() to scope.failedExceptions()
 * }
 * // results == [1, 3], errors.size == 1
 * ```
 *
 * @param T subtask가 반환하는 타입
 * @param R 블록이 반환하는 결과 타입
 * @param name scope 이름 (디버깅용, 기본값: null)
 * @param factory Virtual Thread 팩토리 (기본값: `VirtualThreads.threadFactory("sts-supervised-")`)
 * @param block scope를 인자로 받아 서브 작업을 fork하고 결과를 반환하는 블록
 * @return [block]의 실행 결과
 */
fun <T, R> structuredTaskScopeSupervised(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-supervised-"),
    block: (scope: StructuredTaskScopeSupervised<T>) -> R,
): R = StructuredTaskScopes.supervised(name, factory, block)

/**
 * [StructuredTaskScope.ShutdownOnFailure] 를 사용하여 구조화된 작업을 수행합니다.
 *
 * @deprecated [structuredTaskScopeFailFast]를 사용하세요. factory 기본값이 추가되고 이름이 의도를 더 명확히 표현합니다.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "structuredTaskScopeFailFast()를 사용하세요.",
    replaceWith = ReplaceWith(
        "structuredTaskScopeFailFast(name, factory, block)",
        "io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFailFast"
    )
)
fun <T> structuredTaskScopeAll(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-all-"),
    block: (scope: StructuredTaskScopeAll) -> T,
): T = StructuredTaskScopes.all(name, factory, block)

/**
 * [StructuredTaskScope.ShutdownOnSuccess] 를 사용하여 구조화된 작업을 수행합니다.
 *
 * @deprecated [structuredTaskScopeFirstSuccess]를 사용하세요. factory 기본값이 추가되고 이름이 의도를 더 명확히 표현합니다.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "structuredTaskScopeFirstSuccess()를 사용하세요.",
    replaceWith = ReplaceWith(
        "structuredTaskScopeFirstSuccess(name, factory, block)",
        "io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFirstSuccess"
    )
)
fun <T> structuredTaskScopeAny(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-any-"),
    block: (scope: StructuredTaskScopeAny<T>) -> T,
): T = StructuredTaskScopes.any(name, factory, block)
