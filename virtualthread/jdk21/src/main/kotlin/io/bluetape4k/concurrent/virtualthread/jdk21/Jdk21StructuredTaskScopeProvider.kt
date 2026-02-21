package io.bluetape4k.concurrent.virtualthread.jdk21

import io.bluetape4k.concurrent.virtualthread.StructuredSubtask
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAll
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeAny
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import java.util.concurrent.Callable
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory

/**
 * Java 21 StructuredTaskScope 구현체입니다.
 */
class Jdk21StructuredTaskScopeProvider: StructuredTaskScopeProvider {

    companion object: KLoggingChannel() {
        const val PROVIDER_NAME = "jdk21-structured-task-scope"
        const val JAVA_VERSION = 21
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
        
        return StructuredTaskScope.ShutdownOnFailure(name, factory).use { scope ->
            block(Jdk21AllScope(scope))
        }
    }

    override fun <T> withAny(
        name: String?,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T {
        log.debug { "첫번째로 완료된 subtask의 결과를 반환합낟." }

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
        override fun <T> fork(task: () -> T): StructuredSubtask<T> {
            log.debug { "Add sub task..." }
            return Jdk21Subtask(delegate.fork(Callable { task() }))
        }

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
        override fun <V: T> fork(task: () -> V): StructuredSubtask<V> {
            log.debug { "Add sub task..." }
            return Jdk21Subtask(delegate.fork(Callable<T> { task() }) as StructuredTaskScope.Subtask<V>)
        }

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
