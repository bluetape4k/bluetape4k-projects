package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.concurrent.virtualthread.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory
import java.util.function.Function

/**
 * Java 25 StructuredTaskScope 구현체입니다.
 */
class Jdk25StructuredTaskScopeProvider: StructuredTaskScopeProvider {

    override val providerName: String = "jdk25-structured-scope"
    override val priority: Int = 25

    override fun isSupported(): Boolean = Runtime.version().feature() >= 25

    override fun <T> withAll(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T {
        val scope = StructuredTaskScope.open<Any?, Void>(
            StructuredTaskScope.Joiner.awaitAll(),
            configure(name, factory)
        )
        return scope.use { block(Jdk25AllScope(it)) }
    }

    override fun <T> withAny(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T {
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
            var configured: StructuredTaskScope.Configuration = conf.withThreadFactory(factory)!!
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

        fun exceptionOrNull(): Throwable? = when (delegate.state()) {
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
            val subtask = Jdk25Subtask(delegate.fork(Callable { task() }))
            subtasks += subtask
            return subtask
        }

        override fun join(): StructuredTaskScopeAll {
            delegate.join()
            return this
        }

        override fun throwIfFailed(): StructuredTaskScopeAll {
            val firstFailure = subtasks.asSequence()
                .mapNotNull { it.exceptionOrNull() }
                .firstOrNull()

            if (firstFailure != null) {
                throw ExecutionException(firstFailure)
            }
            return this
        }

        override fun close() {
            delegate.close()
        }
    }

    private class Jdk25AnyScope<T>(
        private val delegate: StructuredTaskScope<T, T>,
    ): StructuredTaskScopeAny<T> {
        private var joinedResult: Result<T>? = null

        @Suppress("UNCHECKED_CAST")
        override fun <V: T> fork(task: () -> V): StructuredSubtask<V> {
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
