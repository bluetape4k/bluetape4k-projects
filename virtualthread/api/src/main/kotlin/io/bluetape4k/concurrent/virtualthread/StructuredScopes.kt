package io.bluetape4k.concurrent.virtualthread

import java.util.*
import java.util.concurrent.ThreadFactory

/**
 * 구조화된 동시성의 Subtask 추상화입니다.
 */
interface StructuredSubtask<T> {
    fun get(): T
}

/**
 * ShutdownOnFailure 성격의 Scope 추상화입니다.
 */
interface StructuredTaskScopeAll: AutoCloseable {
    fun <T> fork(task: () -> T): StructuredSubtask<T>
    fun join(): StructuredTaskScopeAll
    fun throwIfFailed(handler: (e: Throwable) -> Unit = {}): StructuredTaskScopeAll
    override fun close()
}

/**
 * ShutdownOnSuccess 성격의 Scope 추상화입니다.
 */
interface StructuredTaskScopeAny<T>: AutoCloseable {
    fun <V: T> fork(task: () -> V): StructuredSubtask<V>
    fun join(): StructuredTaskScopeAny<T>
    fun result(mapper: (Throwable) -> RuntimeException): T
    override fun close()
}

/**
 * JDK별 StructuredTaskScope 구현 제공자입니다.
 */
interface StructuredTaskScopeProvider {
    val providerName: String
    val priority: Int

    fun isSupported(): Boolean

    fun <T> withAll(
        name: String? = null,
        factory: ThreadFactory = Thread.ofVirtual().factory(),
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T

    fun <T> withAny(
        name: String? = null,
        factory: ThreadFactory = Thread.ofVirtual().factory(),
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T
}

/**
 * 현재 런타임에 맞는 StructuredTaskScope 구현을 선택합니다.
 */
object StructuredTaskScopes {

    private val providers: List<StructuredTaskScopeProvider> by lazy {
        val loader = ServiceLoader.load(StructuredTaskScopeProvider::class.java)
        val iterator = loader.iterator()
        val discovered = mutableListOf<StructuredTaskScopeProvider>()

        while (true) {
            val provider = runCatching {
                if (!iterator.hasNext()) return@runCatching null
                iterator.next()
            }.getOrNull() ?: break

            runCatching {
                if (provider.isSupported()) {
                    discovered += provider
                }
            }
        }

        discovered.sortedByDescending { it.priority }
    }

    fun provider(): StructuredTaskScopeProvider {
        return providers.firstOrNull()
            ?: error("No StructuredTaskScopeProvider available for current runtime.")
    }

    fun providerName(): String = provider().providerName

    fun <T> all(
        name: String? = null,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T = provider().withAll(name, factory, block)

    fun <T> any(
        name: String? = null,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T = provider().withAny(name, factory, block)
}
