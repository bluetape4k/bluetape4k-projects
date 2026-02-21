package io.bluetape4k.concurrent.virtualthread.jdk21

import io.bluetape4k.concurrent.virtualthread.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider
import java.util.concurrent.Callable
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory

/**
 * Java 21 StructuredTaskScope 구현체입니다.
 */
class Jdk21StructuredTaskScopeProvider: StructuredTaskScopeProvider {

    override val providerName: String = "jdk21-structured-scope"
    override val priority: Int = 21

    override fun isSupported(): Boolean = Runtime.version().feature() >= 21

    override fun <T> withAll(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T {
        return StructuredTaskScope.ShutdownOnFailure(name, factory).use { scope ->
            block(Jdk21AllScope(scope))
        }
    }

    override fun <T> withAny(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T {
        return StructuredTaskScope.ShutdownOnSuccess<T>(name, factory).use { scope ->
            block(Jdk21AnyScope(scope))
        }
    }

    private class Jdk21Subtask<T>(
        private val subtask: StructuredTaskScope.Subtask<T>,
    ): StructuredSubtask<T> {
        override fun get(): T = subtask.get()
    }

    private class Jdk21AllScope(
        private val delegate: StructuredTaskScope.ShutdownOnFailure,
    ): StructuredTaskScopeAll {
        override fun <T> fork(task: () -> T): StructuredSubtask<T> =
            Jdk21Subtask(delegate.fork(Callable { task() }))

        override fun join(): StructuredTaskScopeAll {
            delegate.join()
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
        override fun <V: T> fork(task: () -> V): StructuredSubtask<V> =
            Jdk21Subtask(delegate.fork(Callable<T> { task() }) as StructuredTaskScope.Subtask<V>)

        override fun join(): StructuredTaskScopeAny<T> {
            delegate.join()
            return this
        }

        override fun result(mapper: (Throwable) -> RuntimeException): T =
            delegate.result(mapper)

        override fun close() {
            delegate.close()
        }
    }
}
