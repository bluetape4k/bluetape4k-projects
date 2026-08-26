package io.bluetape4k.concurrent.virtualthread.jdk21

import io.bluetape4k.concurrent.virtualthread.api.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeProvider
import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeSupervised
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory

/**
 * JDK 21 `StructuredTaskScope` API를 사용하는 provider 구현체입니다.
 *
 * ## 동작/계약
 * - `Runtime.version().feature() >= 21`일 때 지원 대상으로 판단합니다.
 * - `withAll`은 `ShutdownOnFailure`, `withAny`는 `ShutdownOnSuccess`에 위임합니다.
 * - scope는 `use`로 감싸 실행되어 블록 종료 시 자동 close 됩니다.
 *
 * ```kotlin
 * val provider = Jdk21StructuredTaskScopeProvider()
 * val result = provider.withAll { scope ->
 *     val a = scope.fork { 1 }
 *     val b = scope.fork { 2 }
 *     scope.join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // result == 3
 * ```
 */
class Jdk21StructuredTaskScopeProvider: StructuredTaskScopeProvider {

    companion object: KLoggingChannel() {
        /** provider 식별 이름입니다. */
        const val PROVIDER_NAME = "jdk21-structured-task-scope"

        /** 지원 기준 JDK feature 버전입니다. */
        const val JAVA_VERSION = 21

        /** provider 우선순위 값입니다. */
        const val PRIORITY = JAVA_VERSION
    }

    override val providerName: String = PROVIDER_NAME
    override val priority: Int = PRIORITY

    /**
     * 현재 JVM이 JDK 21 이상인지 확인합니다.
     *
     * ## 동작/계약
     * - feature 버전 비교만 수행하며 추가 reflective 체크는 하지 않습니다.
     *
     * ```kotlin
     * val supported = Jdk21StructuredTaskScopeProvider().isSupported()
     * // supported == (Runtime.version().feature() >= 21)
     * ```
     */
    override fun isSupported(): Boolean = Runtime.version().feature() >= JAVA_VERSION

    /**
     * 실패 전파형(scope-all) 블록을 실행합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `StructuredTaskScope.ShutdownOnFailure`를 생성합니다.
     * - [scope.join()][StructuredTaskScopeAll.join] 후 [scope.throwIfFailed()][StructuredTaskScopeAll.throwIfFailed] 호출 시 첫 실패 예외를 전파합니다.
     *
     * ```kotlin
     * val result = Jdk21StructuredTaskScopeProvider().withAll { scope ->
     *     val a = scope.fork { 1 }
     *     val b = scope.fork { 2 }
     *     scope.join().throwIfFailed()
     *     a.get() + b.get()
     * }
     * // result == 3
     * ```
     */
    override fun <T> withAll(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T {
        log.debug { "모든 subtask 가 완료될 때까지 기다립니다..." }

        return StructuredTaskScope.ShutdownOnFailure(name, factory).use { scope ->
            block(Jdk21AllScope(scope))
        }
    }

    /**
     * 성공 우선형(scope-any) 블록을 실행합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `StructuredTaskScope.ShutdownOnSuccess`를 생성합니다.
     * - 가장 먼저 성공한 subtask 결과를 [StructuredTaskScopeAny.result]로 반환합니다.
     *
     * ```kotlin
     * val result = Jdk21StructuredTaskScopeProvider().withAny<String> { scope ->
     *     scope.fork { "slow" }
     *     scope.fork { "fast" }
     *     scope.join().result { IllegalStateException(it) }
     * }
     * // result == "fast"
     * ```
     */
    override fun <T> withAny(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T {
        log.debug { "첫번째로 완료된 subtask의 결과를 반환합니다." }

        return StructuredTaskScope.ShutdownOnSuccess<T>(name, factory).use { scope ->
            block(Jdk21AnyScope(scope))
        }
    }

    private class Jdk21Subtask<T>(
        private val delegate: StructuredTaskScope.Subtask<T>,
    ): StructuredSubtask<T> {
        override fun get(): T = delegate.get()
        override fun state(): StructuredTaskScope.Subtask.State = delegate.state()
        override fun exceptionOrNull(): Throwable? = when (delegate.state()) {
            StructuredTaskScope.Subtask.State.FAILED -> delegate.exception()
            else                                     -> null
        }
    }

    private class Jdk21AllScope(
        private val delegate: StructuredTaskScope.ShutdownOnFailure,
    ): StructuredTaskScopeAll {
        override fun <T> fork(task: () -> T): StructuredSubtask<T> {
            log.trace { "Add sub task..." }
            return Jdk21Subtask(delegate.fork(Callable { task() }))
        }

        override fun join(): StructuredTaskScopeAll {
            delegate.join()
            return this
        }

        override fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeAll {
            delegate.joinUntil(deadline)
            return this
        }

        override fun throwIfFailed(handler: (e: Throwable) -> Unit): StructuredTaskScopeAll {
            delegate.throwIfFailed {
                handler(it)
                throw it
            }
            return this
        }

        override fun close() {
            delegate.close()
        }
    }

    private class Jdk21AnyScope<T>(
        private val delegate: StructuredTaskScope.ShutdownOnSuccess<T>,
    ): StructuredTaskScopeAny<T> {
        @Suppress("UNCHECKED_CAST")
        override fun <V: T> fork(task: () -> V): StructuredSubtask<V> {
            log.trace { "Add sub task..." }
            return Jdk21Subtask(delegate.fork(Callable<T> { task() }) as StructuredTaskScope.Subtask<V>)
        }

        override fun join(): StructuredTaskScopeAny<T> {
            delegate.join()
            return this
        }

        override fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeAny<T> {
            delegate.joinUntil(deadline)
            return this
        }

        override fun result(mapper: (Throwable) -> RuntimeException): T =
            delegate.result(mapper)

        override fun close() {
            delegate.close()
        }
    }

    /**
     * JDK21 `StructuredTaskScope` 서브클래스를 사용하여 부분 실패 허용 scope를 실행합니다.
     *
     * ## 동작/계약
     * - `Jdk21SupervisedTaskScope`는 [StructuredTaskScope]를 직접 상속하고 [StructuredTaskScope.handleComplete]를
     *   오버라이드하여 성공/실패 결과를 `CopyOnWriteArrayList`에 thread-safe하게 수집합니다.
     * - 실패한 subtask가 있어도 scope를 종료하지 않고 모든 subtask가 완료될 때까지 기다립니다.
     *
     * ```kotlin
     * val (successes, errors) = Jdk21StructuredTaskScopeProvider().withSupervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
     *     scope.fork { 1 }
     *     scope.fork { throw RuntimeException("fail") }
     *     scope.fork { 3 }
     *     scope.join()
     *     scope.successfulResults() to scope.failedExceptions()
     * }
     * // successes = [1, 3], errors.size = 1
     * ```
     */
    override fun <T, R> withSupervised(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeSupervised<T>) -> R,
    ): R {
        log.debug { "부분 실패를 허용하는 supervised scope를 실행합니다..." }
        return Jdk21SupervisedTaskScope<T>(name, factory).use { taskScope ->
            block(Jdk21SupervisedScope(taskScope))
        }
    }

    /**
     * JDK21에서 부분 실패 허용 scope를 구현하기 위해 [StructuredTaskScope]를 직접 상속합니다.
     * [handleComplete]에서 성공/실패 결과를 `Result<T>`로 통합해 thread-safe하게 수집합니다.
     */
    private class Jdk21SupervisedTaskScope<T>(
        name: String?,
        factory: ThreadFactory,
    ) : StructuredTaskScope<T>(name, factory) {
        private val _results = CopyOnWriteArrayList<Result<T>>()

        override fun handleComplete(subtask: StructuredTaskScope.Subtask<out T>) {
            when (subtask.state()) {
                StructuredTaskScope.Subtask.State.SUCCESS -> _results.add(Result.success(subtask.get()))
                StructuredTaskScope.Subtask.State.FAILED  -> _results.add(Result.failure(subtask.exception()))
                else                                      -> {} // UNAVAILABLE — 취소된 subtask, 무시
            }
        }

        fun results(): List<Result<T>> = _results.toList()
    }

    private class Jdk21SupervisedScope<T>(
        private val delegate: Jdk21SupervisedTaskScope<T>,
    ) : StructuredTaskScopeSupervised<T> {
        override fun fork(task: () -> T): StructuredSubtask<T> {
            log.trace { "Add supervised sub task..." }
            return Jdk21Subtask(delegate.fork(Callable { task() }))
        }

        override fun join(): StructuredTaskScopeSupervised<T> {
            delegate.join()
            return this
        }

        override fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeSupervised<T> {
            delegate.joinUntil(deadline)
            return this
        }

        override fun results(): List<Result<T>> = delegate.results()

        override fun close() {
            delegate.close()
        }
    }
}
