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
 * Java 25 StructuredTaskScope 구현체입니다.
 */
class Jdk25StructuredTaskScopeProvider: StructuredTaskScopeProvider {

    companion object: KLoggingChannel() {
        const val PROVIDER_NAME = "jdk25-structured-task-scope"
        const val JAVA_VERSION = 25
        const val PRIORITY = JAVA_VERSION
    }

    override val providerName: String = PROVIDER_NAME
    override val priority: Int = PRIORITY

    override fun isSupported(): Boolean = Runtime.version().feature() >= JAVA_VERSION

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
