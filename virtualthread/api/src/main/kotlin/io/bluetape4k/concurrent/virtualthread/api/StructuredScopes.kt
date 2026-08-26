package io.bluetape4k.concurrent.virtualthread.api

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
 * - [get]은 subtask 상태에 따라 결과 반환 또는 예외를 던집니다.
 * - 안전한 결과 접근은 [getOrNull]을 사용하세요.
 *
 * ```kotlin
 * val value = StructuredTaskScopes.failFast { scope ->
 *     val task = scope.fork { 1 + 1 }
 *     scope.join().throwIfFailed()
 *     task.get()
 * }
 * // value == 2
 * ```
 */
interface StructuredSubtask<T> {
    /**
     * subtask 결과를 반환합니다.
     *
     * ## 상태별 동작
     * - **SUCCESS**: 결과를 반환합니다.
     * - **FAILED**: `IllegalStateException`을 던집니다. 실패 원인은 [exceptionOrNull]로 조회하세요.
     * - **UNAVAILABLE** (미완료 / 취소 / [join] 이전 호출): `IllegalStateException`을 던집니다.
     *
     * @throws IllegalStateException FAILED 또는 UNAVAILABLE 상태, 또는 [join] 이전 호출 시
     * @see exceptionOrNull
     * @see getOrNull
     */
    fun get(): T

    /** subtask 현재 상태를 반환합니다. */
    fun state(): StructuredTaskScope.Subtask.State

    /** subtask 실패 원인을 반환하고, 실패하지 않았으면 `null`을 반환합니다. */
    fun exceptionOrNull(): Throwable?

    /**
     * subtask 결과를 안전하게 반환합니다. 결과를 얻을 수 없으면 `null`을 반환합니다.
     *
     * ## 동작/계약
     * - **전제 조건**: scope owner thread가 [join] 또는 [StructuredTaskScopeAll.joinUntil]을 완료한 이후에 호출하세요.
     * - SUCCESS 상태이면 [get] 결과를 반환합니다.
     * - FAILED 또는 UNAVAILABLE 상태이면 `null`을 반환합니다.
     * - `state() == SUCCESS`이더라도 [join] 이전 호출 시 JDK 내부의 `ensureJoinedIfOwner()` 검사로
     *   `IllegalStateException`이 발생할 수 있으므로 try-catch로 안전하게 처리합니다.
     *
     * ```kotlin
     * StructuredTaskScopes.failFast { scope ->
     *     val task = scope.fork { 42 }
     *     scope.join().throwIfFailed()
     *     task.getOrNull()   // 42
     * }
     * ```
     *
     * @return SUCCESS 상태이면 결과값, 그 외 `null`
     */
    fun getOrNull(): T? {
        return if (state() == StructuredTaskScope.Subtask.State.SUCCESS) {
            try {
                get()
            } catch (_: IllegalStateException) {
                null
            }
        } else null
    }
}

/**
 * 모든 작업 완료를 기다리고, 실패가 있으면 예외를 전파하는 scope 추상화입니다.
 * fail-fast 동작을 합니다 — 하나의 subtask라도 실패하면 나머지를 즉시 중단하고 예외를 전파합니다.
 *
 * ## 동작/계약
 * - [fork]로 추가한 작업들을 [join] 이후 [throwIfFailed]로 일괄 실패 검사할 수 있습니다.
 * - 타임아웃이 필요하면 [joinUntil]을 사용하세요. 데드라인 초과 시 `TimeoutException`이 발생합니다.
 * - [close]는 리소스 정리 및 미완료 작업 취소를 수행할 수 있으므로 `use` 블록 사용을 권장합니다.
 *
 * **의도 명확 별칭**: [StructuredTaskScopeFailFast]를 사용하면 fail-fast 의도가 더 명확해집니다.
 *
 * ```kotlin
 * val sum = StructuredTaskScopes.failFast { scope ->
 *     val a = scope.fork { 1 }
 *     val b = scope.fork { 2 }
 *     scope.join().throwIfFailed()
 *     a.get() + b.get()
 * }
 * // sum == 3
 * ```
 *
 * @see StructuredTaskScopeFailFast
 * @see StructuredTaskScopes.failFast
 */
interface StructuredTaskScopeAll: AutoCloseable {
    /** 새 subtask를 scope에 등록합니다. */
    fun <T> fork(task: () -> T): StructuredSubtask<T>

    /**
     * 등록된 subtask 완료를 대기합니다.
     *
     * **주의**: 타임아웃 없이 호출하면 subtask가 무한 차단될 수 있습니다.
     * 프로덕션 코드에서는 [joinUntil]을 사용하세요.
     */
    fun join(): StructuredTaskScopeAll

    /**
     * 지정한 데드라인까지 subtask 완료를 대기합니다.
     * 데드라인 초과 시 [java.util.concurrent.TimeoutException]이 발생합니다.
     */
    fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeAll

    /** 실패한 subtask가 있으면 [handler]를 호출한 뒤 예외를 전파합니다. */
    fun throwIfFailed(handler: (e: Throwable) -> Unit = {}): StructuredTaskScopeAll

    /** scope 자원을 정리합니다. */
    override fun close()
}

/**
 * fail-fast 동작을 하는 [StructuredTaskScopeAll]의 의도 명확 별칭입니다.
 *
 * @see StructuredTaskScopeAll
 * @see StructuredTaskScopes.failFast
 */
typealias StructuredTaskScopeFailFast = StructuredTaskScopeAll

/**
 * 첫 성공 결과를 선택하는 scope 추상화입니다.
 * first-success 동작을 합니다 — 첫 번째 성공한 subtask 결과를 반환하고 나머지를 취소합니다.
 *
 * ## 동작/계약
 * - 여러 작업을 [fork]한 뒤 [join]과 [result]를 통해 첫 성공 결과를 얻습니다.
 * - 모든 작업이 실패하면 [result]에서 [mapper]가 만든 RuntimeException이 발생합니다.
 *
 * **의도 명확 별칭**: [StructuredTaskScopeFirstSuccess]를 사용하면 first-success 의도가 더 명확해집니다.
 *
 * ```kotlin
 * val winner = StructuredTaskScopes.firstSuccess<String> { scope ->
 *     scope.fork { "slow" }
 *     scope.fork { "fast" }
 *     scope.join().result { IllegalStateException("all failed: ${it.message}") }
 * }
 * // winner.isNotBlank() == true
 * ```
 *
 * @see StructuredTaskScopeFirstSuccess
 * @see StructuredTaskScopes.firstSuccess
 */
interface StructuredTaskScopeAny<T>: AutoCloseable {
    /** 새 subtask를 scope에 등록합니다. */
    fun <V: T> fork(task: () -> V): StructuredSubtask<V>

    /** 등록된 subtask 완료를 대기합니다. */
    fun join(): StructuredTaskScopeAny<T>

    /**
     * 지정한 데드라인까지 subtask 완료를 대기합니다.
     * 데드라인 초과 시 [java.util.concurrent.TimeoutException]이 발생합니다.
     */
    fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeAny<T>

    /** 첫 성공 결과를 반환하거나 실패 시 [mapper]로 예외를 변환해 던집니다. */
    fun result(mapper: (Throwable) -> RuntimeException): T

    /** scope 자원을 정리합니다. */
    override fun close()
}

/**
 * first-success 동작을 하는 [StructuredTaskScopeAny]의 의도 명확 별칭입니다.
 *
 * @see StructuredTaskScopeAny
 * @see StructuredTaskScopes.firstSuccess
 */
typealias StructuredTaskScopeFirstSuccess<T> = StructuredTaskScopeAny<T>

/**
 * 부분 실패를 허용하는 supervised scope 추상화입니다.
 * 모든 subtask를 실행하고, 실패해도 나머지를 계속 진행합니다.
 * [join] 이후 [results]로 각 subtask 결과를 `Result<T>`로 통합 조회하거나,
 * [successfulResults] / [failedExceptions]로 분리해 사용합니다.
 *
 * ## 동작/계약
 * - [fork]로 추가한 모든 subtask가 완료될 때까지 대기합니다.
 * - subtask 하나가 실패해도 나머지 subtask는 계속 실행됩니다 (fail-fast 아님).
 * - [join] 이후에 [results], [successfulResults], [failedExceptions]를 호출해야 합니다.
 * - 타임아웃이 필요하면 [joinUntil]을 사용하세요. 데드라인 초과 시 `TimeoutException`이 발생합니다.
 *
 * ```kotlin
 * // Result<T> 통합 조회
 * val results = StructuredTaskScopes.supervised<Int, List<Result<Int>>> { scope ->
 *     scope.fork { 1 }
 *     scope.fork { throw RuntimeException("subtask 2 failed") }
 *     scope.fork { 3 }
 *     scope.join()
 *     scope.results()
 * }
 * // results[0] == Result.success(1)
 * // results[1] == Result.failure(RuntimeException("subtask 2 failed"))
 * // results[2] == Result.success(3)
 *
 * // 또는 기존 패턴으로 분리
 * val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<...>> { scope ->
 *     scope.fork { 1 }
 *     scope.fork { throw RuntimeException("subtask 2 failed") }
 *     scope.fork { 3 }
 *     scope.join()
 *     scope.successfulResults() to scope.failedExceptions()
 * }
 * // successes == [1, 3], failures.size == 1
 * ```
 *
 * @param T 각 subtask의 결과 타입
 * @see StructuredTaskScopes.supervised
 */
interface StructuredTaskScopeSupervised<T>: AutoCloseable {
    /** 새 subtask를 scope에 등록합니다. */
    fun fork(task: () -> T): StructuredSubtask<T>

    /**
     * 등록된 모든 subtask 완료를 대기합니다.
     * subtask 실패 시에도 나머지 subtask를 계속 실행합니다.
     */
    fun join(): StructuredTaskScopeSupervised<T>

    /**
     * 지정한 데드라인까지 subtask 완료를 대기합니다.
     * 데드라인 초과 시 [java.util.concurrent.TimeoutException]이 발생합니다.
     */
    fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeSupervised<T>

    /**
     * [join] 이후 모든 subtask 결과를 `Result<T>` 리스트로 반환합니다.
     * fork 순서와 결과 순서는 구현에 따라 다를 수 있습니다.
     *
     * - 성공한 subtask: `Result.success(value)`
     * - 실패한 subtask: `Result.failure(exception)`
     *
     * **전제 조건**: [join] 또는 [joinUntil] 완료 이후에 호출하세요.
     */
    fun results(): List<Result<T>>

    /**
     * [join] 이후 성공한 subtask의 결과 리스트를 반환합니다.
     * [results]에서 파생된 기본 구현입니다.
     *
     * **전제 조건**: [join] 또는 [joinUntil] 완료 이후에 호출하세요.
     */
    fun successfulResults(): List<T> = results().filter { it.isSuccess }.map { it.getOrThrow() }

    /**
     * [join] 이후 실패한 subtask의 예외 리스트를 반환합니다.
     * [results]에서 파생된 기본 구현입니다.
     *
     * **전제 조건**: [join] 또는 [joinUntil] 완료 이후에 호출하세요.
     */
    fun failedExceptions(): List<Throwable> = results().mapNotNull { it.exceptionOrNull() }

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
     * @see withFailFast
     */
    fun <T> withAll(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T

    /**
     * 성공 우선형(scope-any) 블록을 실행합니다.
     *
     * @param name scope 이름(지원 구현에서만 적용)
     * @param factory subtask 실행용 스레드 팩토리
     * @param block 실행 블록
     * @see withFirstSuccess
     */
    fun <T> withAny(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T

    /**
     * 실패 전파형(fail-fast) 블록을 실행합니다.
     * 기본 구현은 [withAll]에 위임합니다.
     *
     * @param name scope 이름
     * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
     * @param block 실행 블록
     * @see withAll
     */
    fun <T> withFailFast(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeFailFast) -> T,
    ): T = withAll(name, factory, block)

    /**
     * 성공 우선형(first-success) 블록을 실행합니다.
     * 기본 구현은 [withAny]에 위임합니다.
     *
     * @param name scope 이름
     * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
     * @param block 실행 블록
     * @see withAny
     */
    fun <T> withFirstSuccess(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
    ): T = withAny(name, factory, block)

    /**
     * 부분 실패 허용형(supervised) 블록을 실행합니다.
     * 모든 subtask를 실행하고, 실패 여부와 관계없이 완료를 대기합니다.
     *
     * @param name scope 이름
     * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
     * @param block 실행 블록 — [StructuredTaskScopeSupervised.join] 이후 [StructuredTaskScopeSupervised.results]로
     *   `Result<T>` 통합 조회하거나 [StructuredTaskScopeSupervised.successfulResults] / [StructuredTaskScopeSupervised.failedExceptions]로 분리
     * @see StructuredTaskScopes.supervised
     */
    fun <T, R> withSupervised(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeSupervised<T>) -> R,
    ): R
}

/**
 * 런타임에 맞는 [StructuredTaskScopeProvider]를 선택해 구조화된 동시성 진입 API를 제공합니다.
 *
 * ## 동작/계약
 * - ServiceLoader provider를 [StructuredTaskScopeProvider.priority] 내림차순으로 정렬해 선택합니다.
 * - 선택 가능한 provider가 없으면 즉시 예외를 발생시킵니다.
 * - [failFast]/[firstSuccess]는 선택된 provider 구현에 위임됩니다.
 *
 * ```kotlin
 * val result = StructuredTaskScopes.failFast { scope ->
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
        discoverStructuredTaskScopeProviders(loader.iterator())
    }

    internal fun discoverStructuredTaskScopeProviders(
        iterator: Iterator<StructuredTaskScopeProvider>,
    ): List<StructuredTaskScopeProvider> {
        val discovered = mutableListOf<StructuredTaskScopeProvider>()

        while (true) {
            val hasNext = runCatching {
                iterator.hasNext()
            }.onFailure { error ->
                log.warn(error) { "Stopping StructuredTaskScopeProvider discovery after ServiceLoader.hasNext() failed." }
            }.getOrNull() ?: break
            if (!hasNext) {
                break
            }
            // A broken service entry must not hide later providers that are valid for this runtime.
            val provider = runCatching {
                iterator.next()
            }.onFailure { error ->
                log.warn(error) { "Skipping failed StructuredTaskScopeProvider entry." }
            }.getOrNull() ?: continue

            runCatching {
                if (provider.isSupported()) {
                    discovered += provider
                    log.debug { "Discovered StructuredTaskScopeProvider: ${provider.providerName} (priority: ${provider.priority})" }
                }
            }.onFailure { error ->
                log.warn(error) { "Failed to check StructuredTaskScopeProvider: ${provider.javaClass.name}" }
            }
        }

        return discovered.sortedByDescending { it.priority }
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
     * 실패 전파형(fail-fast) scope 블록을 실행합니다.
     * 하나의 subtask라도 실패하면 나머지를 즉시 중단하고 예외를 전파합니다.
     *
     * ## 동작/계약
     * - block 내부에서 [StructuredTaskScopeFailFast] API로 subtask를 등록/대기/실패 검사합니다.
     * - 실제 scope 구현은 선택된 provider에 의해 결정됩니다.
     *
     * ```kotlin
     * val sum = StructuredTaskScopes.failFast { scope ->
     *     val a = scope.fork { 1 }
     *     val b = scope.fork { 2 }
     *     scope.join().throwIfFailed()
     *     a.get() + b.get()
     * }
     * // sum == 3
     * ```
     *
     * @param name scope 이름 (디버깅용, 기본값: null)
     * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
     * @param block scope 실행 블록
     * @return [block]의 실행 결과
     */
    fun <T> failFast(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeFailFast) -> T,
    ): T = provider().withFailFast(name, factory, block)

    /**
     * 성공 우선형(first-success) scope 블록을 실행합니다.
     * 첫 번째 성공한 subtask 결과를 반환하고 나머지를 취소합니다.
     *
     * ## 동작/계약
     * - block 내부에서 [StructuredTaskScopeFirstSuccess] API로 첫 성공 결과를 선택합니다.
     * - 모든 subtask가 실패하면 [StructuredTaskScopeAny.result]에서 mapper 예외가 발생합니다.
     *
     * ```kotlin
     * val winner = StructuredTaskScopes.firstSuccess<String> { scope ->
     *     scope.fork { "slow" }
     *     scope.fork { "fast" }
     *     scope.join().result { IllegalStateException("all failed: ${it.message}") }
     * }
     * // winner.isNotBlank() == true
     * ```
     *
     * @param name scope 이름 (디버깅용, 기본값: null)
     * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
     * @param block scope 실행 블록
     * @return 가장 먼저 성공한 subtask의 결과
     */
    fun <T> firstSuccess(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
    ): T = provider().withFirstSuccess(name, factory, block)

    /**
     * 부분 실패 허용형(supervised) scope 블록을 실행합니다.
     * 하나의 subtask가 실패해도 나머지 subtask를 계속 실행합니다.
     *
     * ## 동작/계약
     * - block 내부에서 [StructuredTaskScopeSupervised] API로 subtask를 등록·대기·결과 조회합니다.
     * - [StructuredTaskScopeSupervised.join] 이후 [StructuredTaskScopeSupervised.results]로
     *   각 subtask 결과를 `Result<T>`로 통합 조회하거나,
     *   [StructuredTaskScopeSupervised.successfulResults] / [StructuredTaskScopeSupervised.failedExceptions]로 분리합니다.
     * - 실제 scope 구현은 선택된 provider에 의해 결정됩니다.
     *
     * ```kotlin
     * // Result<T> 통합 조회
     * val allResults = StructuredTaskScopes.supervised<Int, List<Result<Int>>> { scope ->
     *     scope.fork { 1 }
     *     scope.fork { throw RuntimeException("subtask 2 failed") }
     *     scope.fork { 3 }
     *     scope.join()
     *     scope.results()
     * }
     * // allResults.filter { it.isSuccess }.map { it.getOrThrow() } == [1, 3]
     *
     * // 분리 패턴
     * val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
     *     scope.fork { 1 }
     *     scope.fork { throw RuntimeException("subtask 2 failed") }
     *     scope.fork { 3 }
     *     scope.join()
     *     scope.successfulResults() to scope.failedExceptions()
     * }
     * // successes == [1, 3], failures.size == 1
     * ```
     *
     * @param name scope 이름 (디버깅용, 기본값: null)
     * @param factory subtask 실행용 스레드 팩토리 (기본값: [VirtualThreads.threadFactory])
     * @param block scope 실행 블록
     * @return [block]의 실행 결과
     */
    fun <T, R> supervised(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeSupervised<T>) -> R,
    ): R = provider().withSupervised(name, factory, block)

    /**
     * 실패 전파형(all) scope 블록을 실행합니다.
     *
     * @deprecated [failFast]를 사용하세요. factory 기본값이 추가되고 이름이 의도를 더 명확히 표현합니다.
     */
    @Deprecated(
        message = "failFast()를 사용하세요.",
        replaceWith = ReplaceWith(
            "StructuredTaskScopes.failFast(name, factory, block)",
            "io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopes"
        )
    )
    fun <T> all(
        name: String? = null,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T = provider().withAll(name, factory, block)

    /**
     * 성공 우선형(any) scope 블록을 실행합니다.
     *
     * @deprecated [firstSuccess]를 사용하세요. factory 기본값이 추가되고 이름이 의도를 더 명확히 표현합니다.
     */
    @Deprecated(
        message = "firstSuccess()를 사용하세요.",
        replaceWith = ReplaceWith(
            "StructuredTaskScopes.firstSuccess(name, factory, block)",
            "io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopes"
        )
    )
    fun <T> any(
        name: String? = null,
        factory: ThreadFactory,
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T = provider().withAny(name, factory, block)
}
