package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes.provider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.util.*
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory

/**
 * 구조화된 동시성에서 개별 fork 작업 결과를 표현하는 추상화입니다.
 *
 * ## 동작/계약
 * - 구현체는 JDK별 `StructuredTaskScope.Subtask`를 감싸 동일 API를 제공합니다.
 * - [get]은 성공 상태에서 결과를 반환하고 실패 상태에서는 예외를 전파할 수 있습니다.
 *
 * ```kotlin
 * val value = StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
 *     val task = scope.fork { 1 + 1 }
 *     scope.join().throwIfFailed()
 *     task.get()
 * }
 * // value == 2
 * ```
 */
interface StructuredSubtask<T> {
    /** subtask 성공 결과를 반환합니다. */
    fun get(): T
    /** subtask 현재 상태를 반환합니다. */
    fun state(): StructuredTaskScope.Subtask.State
    /** subtask 실패 원인을 반환하고, 실패하지 않았으면 `null`을 반환합니다. */
    fun exceptionOrNull(): Throwable?
}

/**
 * 모든 작업 완료를 기다리고, 실패가 있으면 예외를 전파하는 scope 추상화입니다.
 *
 * ## 동작/계약
 * - [fork]로 추가한 작업들을 [join] 이후 [throwIfFailed]로 일괄 실패 검사할 수 있습니다.
 * - [close]는 리소스 정리 및 미완료 작업 취소를 수행할 수 있으므로 `use` 블록 사용을 권장합니다.
 *
 * ```kotlin
 * val sum = StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
 *     val a = scope.fork { 1 }
 *     val b = scope.fork { 2 }
 *     scope.join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // sum == 3
 * ```
 */
interface StructuredTaskScopeAll: AutoCloseable {
    /** 새 subtask를 scope에 등록합니다. */
    fun <T> fork(task: () -> T): StructuredSubtask<T>
    /** 등록된 subtask 완료를 대기합니다. */
    fun join(): StructuredTaskScopeAll
    /** 실패한 subtask가 있으면 [handler]를 호출한 뒤 예외를 전파합니다. */
    fun throwIfFailed(handler: (e: Throwable) -> Unit = {}): StructuredTaskScopeAll
    /** scope 자원을 정리합니다. */
    override fun close()
}

/**
 * 첫 성공 결과를 선택하는 scope 추상화입니다.
 *
 * ## 동작/계약
 * - 여러 작업을 [fork]한 뒤 [join]과 [result]를 통해 첫 성공 결과를 얻습니다.
 * - 모든 작업이 실패하면 [result]에서 [mapper]가 만든 RuntimeException이 발생합니다.
 *
 * ```kotlin
 * val winner = StructuredTaskScopes.any<String>(factory = Thread.ofVirtual().factory()) { scope ->
 *     scope.fork { "slow" }
 *     scope.fork { "fast" }
 *     scope.join().result { IllegalStateException(it) }
 * }
 * // winner.isNotBlank() == true
 * ```
 */
interface StructuredTaskScopeAny<T>: AutoCloseable {
    /** 새 subtask를 scope에 등록합니다. */
    fun <V: T> fork(task: () -> V): StructuredSubtask<V>
    /** 등록된 subtask 완료를 대기합니다. */
    fun join(): StructuredTaskScopeAny<T>
    /** 첫 성공 결과를 반환하거나 실패 시 [mapper]로 예외를 변환해 던집니다. */
    fun result(mapper: (Throwable) -> RuntimeException): T
    /** scope 자원을 정리합니다. */
    override fun close()
}

/**
 * JDK별 StructuredTaskScope 구현체를 제공하는 SPI 인터페이스입니다.
 *
 * ## 동작/계약
 * - 구현체는 [isSupported]로 현재 런타임 지원 여부를 판단해야 합니다.
 * - [priority]가 높은 구현체가 우선 선택됩니다.
 *
 * ```kotlin
 * val provider = StructuredTaskScopes.provider()
 * // provider.providerName.isNotBlank() == true
 * ```
 */
interface StructuredTaskScopeProvider {
    /** provider 식별 이름입니다. */
    val providerName: String
    /** provider 선택 우선순위입니다. */
    val priority: Int

    /** 현재 JVM에서 provider 사용 가능 여부를 반환합니다. */
    fun isSupported(): Boolean

    /**
     * 실패 전파형(scope-all) 블록을 실행합니다.
     *
     * @param name scope 이름(지원 구현에서만 적용)
     * @param factory subtask 실행용 스레드 팩토리
     * @param block 실행 블록
     */
    fun <T> withAll(
        name: String? = null,
        factory: ThreadFactory = Thread.ofVirtual().factory(),
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T

    /**
     * 성공 우선형(scope-any) 블록을 실행합니다.
     *
     * @param name scope 이름(지원 구현에서만 적용)
     * @param factory subtask 실행용 스레드 팩토리
     * @param block 실행 블록
     */
    fun <T> withAny(
        name: String? = null,
        factory: ThreadFactory = Thread.ofVirtual().factory(),
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T
}

/**
 * 런타임에 맞는 [StructuredTaskScopeProvider]를 선택해 구조화된 동시성 진입 API를 제공합니다.
 *
 * ## 동작/계약
 * - ServiceLoader provider를 [StructuredTaskScopeProvider.priority] 내림차순으로 정렬해 선택합니다.
 * - 선택 가능한 provider가 없으면 즉시 예외를 발생시킵니다.
 * - `all`/`any`는 선택된 provider 구현에 위임됩니다.
 *
 * ```kotlin
 * val result = StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
 *     val a = scope.fork { 1 }
 *     val b = scope.fork { 2 }
 *     scope.join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // result == 3
 * ```
 */
object StructuredTaskScopes: KLogging() {

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
                    log.debug { "Discovered StructuredTaskScopeProvider: ${provider.providerName} (priority: ${provider.priority})" }
                }
            }.onFailure { error ->
                log.warn(error) { "Failed to check StructuredTaskScopeProvider: ${provider.javaClass.name}" }
            }
        }

        discovered.sortedByDescending { it.priority }
    }

    /**
     * 현재 런타임에서 사용할 provider를 반환합니다.
     *
     * ## 동작/계약
     * - 지원되는 provider가 하나도 없으면 `IllegalStateException`을 발생시킵니다.
     *
     * ```kotlin
     * val provider = StructuredTaskScopes.provider()
     * // provider.providerName.isNotBlank() == true
     * ```
     *
     * @throws IllegalStateException 사용 가능한 provider가 없을 때 발생합니다.
     */
    fun provider(): StructuredTaskScopeProvider {
        return providers.firstOrNull()
            ?: error("No StructuredTaskScopeProvider available for current runtime.")
    }

    /**
     * 선택된 provider 이름을 반환합니다.
     *
     * ## 동작/계약
     * - [provider]의 [StructuredTaskScopeProvider.providerName]을 그대로 반환합니다.
     *
     * ```kotlin
     * val name = StructuredTaskScopes.providerName()
     * // name.isNotBlank() == true
     * ```
     */
    fun providerName(): String = provider().providerName

    /**
     * 실패 전파형(all) scope 블록을 실행합니다.
     *
     * ## 동작/계약
     * - block 내부에서 [StructuredTaskScopeAll] API로 subtask를 등록/대기/실패 검사합니다.
     * - 실제 scope 구현은 선택된 provider에 의해 결정됩니다.
     *
     * ```kotlin
     * val value = StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
     *     val task = scope.fork { 42 }
     *     scope.join().throwIfFailed()
     *     task.get()
     * }
     * // value == 42
     * ```
     *
     * @param name scope 이름
     * @param factory subtask 실행용 스레드 팩토리
     * @param block scope 실행 블록
     */
    fun <T> all(
        name: String? = null,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T = provider().withAll(name, factory, block)

    /**
     * 성공 우선형(any) scope 블록을 실행합니다.
     *
     * ## 동작/계약
     * - block 내부에서 [StructuredTaskScopeAny] API로 첫 성공 결과를 선택합니다.
     * - 모든 작업 실패 시 [StructuredTaskScopeAny.result]에서 mapper 예외가 발생합니다.
     *
     * ```kotlin
     * val value = StructuredTaskScopes.any<String>(factory = Thread.ofVirtual().factory()) { scope ->
     *     scope.fork { "fast" }
     *     scope.join().result { IllegalStateException(it) }
     * }
     * // value == "fast"
     * ```
     *
     * @param name scope 이름
     * @param factory subtask 실행용 스레드 팩토리
     * @param block scope 실행 블록
     */
    fun <T> any(
        name: String? = null,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T = provider().withAny(name, factory, block)
}
