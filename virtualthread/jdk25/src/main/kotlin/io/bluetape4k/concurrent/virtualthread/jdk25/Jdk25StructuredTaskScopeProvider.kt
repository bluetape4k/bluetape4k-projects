package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.concurrent.virtualthread.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeSupervised
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Function

/**
 * JDK 25 `StructuredTaskScope.open` API를 사용하는 provider 구현체입니다.
 *
 * ## 동작/계약
 * - `Runtime.version().feature() >= 25`일 때 지원 대상으로 판단합니다.
 * - `withAll`은 `Joiner.awaitAll`, `withAny`는 `Joiner.anySuccessfulResultOrThrow`를 사용합니다.
 * - [configure]에서 전달된 ThreadFactory를 반드시 설정하며 실패 시 `IllegalStateException`이 발생합니다.
 *
 * ```kotlin
 * val provider = Jdk25StructuredTaskScopeProvider()
 * val result = provider.withAll { scope ->
 *     val a = scope.fork { 10 }
 *     val b = scope.fork { 20 }
 *     scope.join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // result == 30
 * ```
 */
class Jdk25StructuredTaskScopeProvider: StructuredTaskScopeProvider {

    companion object: KLoggingChannel() {
        /** provider 식별 이름입니다. */
        const val PROVIDER_NAME = "jdk25-structured-task-scope"

        /** 지원 기준 JDK feature 버전입니다. */
        const val JAVA_VERSION = 25

        /** provider 우선순위 값입니다. */
        const val PRIORITY = JAVA_VERSION

        /**
         * JDK25 scope 구현체들에서 공유하는 interrupt 기반 타임아웃 패턴입니다.
         *
         * [joinAction]이 실행되는 동안 [deadline]이 초과되면 owner thread를 interrupt하여
         * [InterruptedException]을 발생시키고, 이를 [TimeoutException]으로 변환합니다.
         *
         * ## 계약
         * - [joinAction]은 [InterruptedException]을 catch하지 않고 전파해야 합니다.
         * - JDK25 `StructuredTaskScope`는 `joinUntil(Instant)` API가 없으므로 이 패턴으로 대체합니다.
         *
         * @throws TimeoutException [deadline] 초과 시
         */
        internal fun interruptJoinUntil(
            deadline: Instant,
            threadName: String,
            joinAction: () -> Unit,
        ) {
            val remaining = Duration.between(Instant.now(), deadline)
            if (remaining.isNegative || remaining.isZero) {
                throw TimeoutException("Deadline already passed")
            }
            val ownerThread = Thread.currentThread()
            val scheduler = ScheduledThreadPoolExecutor(1) { r ->
                Thread(r, threadName).apply { isDaemon = true }
            }
            val timeoutFuture = scheduler.schedule(
                { ownerThread.interrupt() },
                remaining.toNanos(),
                TimeUnit.NANOSECONDS
            )
            try {
                joinAction()
            } catch (e: InterruptedException) {
                throw TimeoutException("joinUntil deadline exceeded")
            } finally {
                timeoutFuture.cancel(false)
                scheduler.shutdownNow()
                // finally 이후 scheduled interrupt가 늦게 발화하더라도 interrupt 상태가
                // caller thread로 누출되지 않도록 항상 clear
                Thread.interrupted()
            }
        }
    }

    override val providerName: String = PROVIDER_NAME
    override val priority: Int = PRIORITY

    /**
     * 현재 JVM이 JDK 25 이상인지 확인합니다.
     *
     * ## 동작/계약
     * - feature 버전 비교만 수행하며 추가 reflective 체크는 하지 않습니다.
     *
     * ```kotlin
     * val supported = Jdk25StructuredTaskScopeProvider().isSupported()
     * // supported == (Runtime.version().feature() >= 25)
     * ```
     */
    override fun isSupported(): Boolean = Runtime.version().feature() >= JAVA_VERSION

    /**
     * 실패 전파형(fail-fast) 블록을 실행합니다.
     *
     * ## 동작/계약
     * - `Joiner.awaitAllSuccessfulOrThrow()`를 사용하여 subtask 실패 시 나머지를 즉시 취소합니다.
     * - join 후 [StructuredTaskScopeAll.throwIfFailed]로 첫 번째 실패 예외를 전파합니다.
     * - [join]은 subtask 실패 예외를 내부에 저장하여 [throwIfFailed] 호출 시 재발생시킵니다.
     *
     * ```kotlin
     * val result = Jdk25StructuredTaskScopeProvider().withAll { scope ->
     *     val a = scope.fork { 10 }
     *     val b = scope.fork { 20 }
     *     scope.join().throwIfFailed()
     *     a.get() + b.get()
     * }
     * // result == 30
     * ```
     */
    override fun <T> withAll(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T {
        log.debug { "모든 subtask 가 완료될 때까지 기다립니다..." }

        val scope = StructuredTaskScope.open<Any?, Void>(
            StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
            configure(name, factory)
        )
        return scope.use { block(Jdk25AllScope(it)) }
    }

    /**
     * 성공 우선형(scope-any) 블록을 실행합니다.
     *
     * ## 동작/계약
     * - `StructuredTaskScope.open<T, T>(Joiner.anySuccessfulResultOrThrow(), ...)`를 사용합니다.
     * - [StructuredTaskScopeAny.result]에서 join 실패를 `mapper` 예외로 변환합니다.
     *
     * ```kotlin
     * val result = Jdk25StructuredTaskScopeProvider().withAny<String> { scope ->
     *     scope.fork { Thread.sleep(80); "slow" }
     *     scope.fork { Thread.sleep(10); "fast" }
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

        val scope = StructuredTaskScope.open<T, T>(
            StructuredTaskScope.Joiner.anySuccessfulResultOrThrow(),
            configure(name, factory)
        )
        return scope.use { block(Jdk25AnyScope(it)) }
    }

    private fun configure(
        name: String?,
        factory: ThreadFactory,
    ): Function<StructuredTaskScope.Configuration, StructuredTaskScope.Configuration> {
        return Function { conf: StructuredTaskScope.Configuration ->
            var configured: StructuredTaskScope.Configuration = requireNotNull(conf.withThreadFactory(factory)) {
                "Failed to configure ThreadFactory"
            }
            if (!name.isNullOrBlank()) {
                configured = configured.withName(name)
            }
            configured
        }
    }

    private class Jdk25Subtask<T>(
        private val delegate: StructuredTaskScope.Subtask<T>,
    ): StructuredSubtask<T> {
        override fun get(): T = delegate.get()
        override fun state(): StructuredTaskScope.Subtask.State = delegate.state()
        override fun exceptionOrNull(): Throwable? = when (delegate.state()) {
            StructuredTaskScope.Subtask.State.FAILED -> delegate.exception()
            else                                     -> null
        }

        fun isSuccess(): Boolean = delegate.state() == StructuredTaskScope.Subtask.State.SUCCESS
    }

    private class Jdk25AllScope(
        private val delegate: StructuredTaskScope<Any?, Void>,
    ): StructuredTaskScopeAll {
        private val subtasks = CopyOnWriteArrayList<Jdk25Subtask<*>>()
        // awaitAllSuccessfulOrThrow()는 subtask 실패 시 join()에서 예외를 직접 던진다.
        // 기존 API 패턴(join().throwIfFailed()) 유지를 위해 예외를 저장했다가 throwIfFailed에서 재발생
        private var joinException: Throwable? = null

        override fun <T> fork(task: () -> T): StructuredSubtask<T> {
            log.trace { "Add sub task..." }
            val subtask = Jdk25Subtask(delegate.fork(Callable { task() }))
            subtasks += subtask
            return subtask
        }

        override fun join(): StructuredTaskScopeAll {
            try {
                delegate.join()
            } catch (e: Throwable) {
                // awaitAllSuccessfulOrThrow()는 subtask 실패를 FailedException으로 래핑해서 throw.
                // 원본 예외(cause)를 사용자에게 노출하기 위해 언래핑
                joinException = if (e is StructuredTaskScope.FailedException) e.cause ?: e else e
            }
            return this
        }

        override fun joinUntil(deadline: Instant): StructuredTaskScopeAll {
            try {
                interruptJoinUntil(deadline, "jdk25-scope-timeout") { delegate.join() }
            } catch (e: TimeoutException) {
                throw e
            } catch (e: Throwable) {
                joinException = if (e is StructuredTaskScope.FailedException) e.cause ?: e else e
            }
            return this
        }

        override fun throwIfFailed(handler: (e: Throwable) -> Unit): StructuredTaskScopeAll {
            val firstFailure = joinException
                ?: subtasks.firstNotNullOfOrNull { it.exceptionOrNull() }

            if (firstFailure != null) {
                runCatching { handler(firstFailure) }
                throw firstFailure
            }
            return this
        }

        override fun close() {
            try {
                delegate.close()
            } finally {
                subtasks.clear()
            }
        }
    }

    private class Jdk25AnyScope<T>(
        private val delegate: StructuredTaskScope<T, T>,
    ): StructuredTaskScopeAny<T> {
        private var joinedResult: Result<T>? = null

        @Suppress("UNCHECKED_CAST")
        override fun <V: T> fork(task: () -> V): StructuredSubtask<V> {
            log.trace { "Add sub task..." }
            val subtask = Jdk25Subtask(delegate.fork(Callable { task() }))
            return subtask as StructuredSubtask<V>
        }

        override fun join(): StructuredTaskScopeAny<T> {
            joinedResult = runCatching { delegate.join() }
            return this
        }

        override fun joinUntil(deadline: Instant): StructuredTaskScopeAny<T> {
            interruptJoinUntil(deadline, "jdk25-any-scope-timeout") {
                try {
                    joinedResult = Result.success(delegate.join())
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    joinedResult = Result.failure(e)
                }
            }
            return this
        }

        override fun result(mapper: (Throwable) -> RuntimeException): T {
            val result = joinedResult ?: runCatching { delegate.join() }
            return result.getOrElse { throwable ->
                throw mapper(throwable.cause ?: throwable)
            }
        }

        override fun close() {
            delegate.close()
        }
    }

    /**
     * JDK25 `Joiner.awaitAll()`을 사용하여 부분 실패 허용 scope를 실행합니다.
     *
     * ## 동작/계약
     * - `StructuredTaskScope.open<T, Void>(Joiner.awaitAll(), ...)`로 scope를 생성합니다.
     * - `awaitAll()`은 subtask 실패 시에도 나머지를 취소하지 않고 모두 완료까지 기다립니다.
     * - join 후 [Jdk25SupervisedScope.successfulResults]와 [Jdk25SupervisedScope.failedExceptions]로 결과를 분리합니다.
     *
     * ```kotlin
     * val (successes, errors) = Jdk25StructuredTaskScopeProvider().withSupervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
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
        val scope = StructuredTaskScope.open<T, Void>(
            StructuredTaskScope.Joiner.awaitAll(),
            configure(name, factory)
        )
        return scope.use { block(Jdk25SupervisedScope(it)) }
    }

    private class Jdk25SupervisedScope<T>(
        private val delegate: StructuredTaskScope<T, Void>,
    ) : StructuredTaskScopeSupervised<T> {
        private val subtasks = CopyOnWriteArrayList<Jdk25Subtask<T>>()

        override fun fork(task: () -> T): StructuredSubtask<T> {
            log.trace { "Add supervised sub task..." }
            val subtask = Jdk25Subtask(delegate.fork(Callable { task() }))
            subtasks += subtask
            return subtask
        }

        override fun join(): StructuredTaskScopeSupervised<T> {
            delegate.join()
            return this
        }

        override fun joinUntil(deadline: Instant): StructuredTaskScopeSupervised<T> {
            interruptJoinUntil(deadline, "jdk25-supervised-timeout") { delegate.join() }
            return this
        }

        override fun results(): List<Result<T>> = subtasks.map { subtask ->
            when (subtask.state()) {
                StructuredTaskScope.Subtask.State.SUCCESS ->
                    Result.success(subtask.get())
                StructuredTaskScope.Subtask.State.FAILED  ->
                    Result.failure(subtask.exceptionOrNull()!!)
                else ->
                    // UNAVAILABLE: 취소되거나 join 이전 상태 — 결과를 가져오면 IllegalStateException 발생
                    Result.failure(java.util.concurrent.CancellationException("Subtask was cancelled or unavailable"))
            }
        }

        override fun close() {
            try {
                delegate.close()
            } finally {
                subtasks.clear()
            }
        }
    }
}
