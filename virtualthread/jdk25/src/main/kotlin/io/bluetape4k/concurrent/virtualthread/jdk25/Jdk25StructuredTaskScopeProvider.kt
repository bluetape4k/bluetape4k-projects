package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.concurrent.virtualthread.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.util.concurrent.Callable
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory
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
     * 실패 전파형(scope-all) 블록을 실행합니다.
     *
     * ## 동작/계약
     * - `StructuredTaskScope.open<Any?, Void>(Joiner.awaitAll(), ...)`로 scope를 생성합니다.
     * - [StructuredTaskScopeAll.throwIfFailed]는 내부에서 수집한 첫 실패 예외를 전파합니다.
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
            StructuredTaskScope.Joiner.awaitAll(),
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
        private val subtasks = mutableListOf<Jdk25Subtask<*>>()

        override fun <T> fork(task: () -> T): StructuredSubtask<T> {
            log.trace { "Add sub task..." }
            val subtask = Jdk25Subtask(delegate.fork(Callable { task() }))
            subtasks += subtask
            return subtask
        }

        override fun join(): StructuredTaskScopeAll {
            delegate.join()
            return this
        }

        override fun throwIfFailed(handler: (e: Throwable) -> Unit): StructuredTaskScopeAll {
            val firstFailure = subtasks.firstNotNullOfOrNull { it.exceptionOrNull() }

            if (firstFailure != null) {
                handler(firstFailure)
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
}
