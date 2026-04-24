package io.bluetape4k.micrometer.observation.coroutines

import io.bluetape4k.coroutines.reactor.currentReactiveContext
import io.bluetape4k.coroutines.reactor.getOrNull
import io.bluetape4k.micrometer.observation.start
import io.bluetape4k.support.requireNotBlank
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.withContext
import reactor.util.context.Context
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 현재 Coroutine Scope에서 Observation을 가져옵니다.
 * Observation이 없는 경우 null을 반환합니다.
 *
 * ```kotlin
 * withObservationContext("observer.delay", registry) {
 *     val observation = currentObservationInContext()!!
 *     // some code to observe
 *     delay(100.milliseconds)
 * }
 * ```
 *
 * @return 현재 컨텍스트의 [Observation] 또는 null
 */
suspend fun currentObservationInContext(): Observation? =
    currentReactiveContext()?.getOrNull(ObservationThreadLocalAccessor.KEY)

private class ObservationScopeContextElement(
    private val observation: Observation,
): ThreadContextElement<Observation.Scope>, AbstractCoroutineContextElement(Key) {

    companion object Key: CoroutineContext.Key<ObservationScopeContextElement>

    override fun updateThreadContext(context: CoroutineContext): Observation.Scope =
        observation.openScope()

    override fun restoreThreadContext(context: CoroutineContext, oldState: Observation.Scope) {
        oldState.close()
    }
}

private suspend fun Observation.asCoroutineObservationContext(): CoroutineContext {
    val reactorContext = (currentReactiveContext() ?: Context.empty())
        .put(ObservationThreadLocalAccessor.KEY, this)

    return reactorContext.asCoroutineContext() + ObservationScopeContextElement(this)
}

/**
 * 이미 생성된 [Observation]을 suspend 블록에 연결해 실행합니다.
 *
 * ## 동작/계약
 * - 현재 Reactor/Coroutine 컨텍스트에 [Observation]을 바인딩합니다.
 * - 블록 실행 중 예외가 발생하면 Observation에 에러를 기록합니다.
 * - 블록이 끝나면 Observation scope 를 닫고 `stop()`을 호출합니다.
 * - 시작되지 않은 Observation을 전달해도 내부에서 `start()` 후 실행합니다.
 *
 * ```kotlin
 * val registry = ObservationRegistry.create()
 * val observation = Observation.createNotStarted("my-op", registry)
 * val result = observation.observeSuspending { ctx ->
 *     "observed-result"
 * }
 * // result == "observed-result"
 * ```
 *
 * @param T 결과 타입
 * @param block Observation 컨텍스트를 받아 실행할 suspend 블록
 * @return 블록 결과 또는 `null`
 */
suspend inline fun <T: Any> Observation.observeSuspending(
    crossinline block: suspend (Observation.Context) -> T?,
): T? =
    withObservationContextSuspending { ctx: Observation.Context ->
        block(ctx)
    }

/**
 * [observeSuspending] 결과를 [Result] 로 감싸 예외를 호출자에게 위임하지 않습니다.
 *
 * ```kotlin
 * val registry = ObservationRegistry.create()
 * val observation = Observation.createNotStarted("my-op", registry)
 * val result = observation.tryObserveSuspending { ctx ->
 *     "observed-result"
 * }
 * // result.isSuccess == true
 * // result.getOrNull() == "observed-result"
 * ```
 *
 * @param T 결과 타입
 * @param block Observation 컨텍스트를 받아 실행할 suspend 블록
 * @return 성공 시 블록 결과, 실패 시 예외를 담은 [Result]
 */
suspend inline fun <T: Any> Observation.tryObserveSuspending(
    crossinline block: suspend (Observation.Context) -> T?,
): Result<T> =
    runCatching {
        withObservationContextSuspending { ctx: Observation.Context ->
            block(ctx)
        } ?: throw NoSuchElementException()
    }

/**
 * 이름을 기준으로 새 [Observation]을 만들고 suspend 블록을 실행합니다.
 *
 * ```kotlin
 * val registry = ObservationRegistry.create()
 * val result = withObservationSuspending("user.fetch", registry) {
 *     "user-data"
 * }
 * // result == "user-data"
 * ```
 *
 * @param T 결과 타입
 * @param name Observation 이름
 * @param registry Observation 등록 대상 [ObservationRegistry]
 * @param block Observation 이 바인딩된 상태로 실행할 suspend 블록
 * @return 블록 결과 또는 `null`
 */
suspend inline fun <T: Any> withObservationSuspending(
    name: String,
    registry: ObservationRegistry,
    crossinline block: suspend () -> T?,
): T? =
    withObservationContextSuspending(name, registry) {
        block()
    }

/**
 * [withObservationSuspending] 결과를 [Result] 로 감싸 반환합니다.
 *
 * ```kotlin
 * val registry = ObservationRegistry.create()
 * val result = tryWithObservationSuspending("user.fetch", registry) {
 *     "user-data"
 * }
 * // result.isSuccess == true
 * // result.getOrNull() == "user-data"
 * ```
 *
 * @param T 결과 타입
 * @param name Observation 이름
 * @param registry Observation 등록 대상 [ObservationRegistry]
 * @param block Observation 이 바인딩된 상태로 실행할 suspend 블록
 * @return 성공 시 블록 결과, 실패 시 예외를 담은 [Result]
 */
suspend inline fun <T: Any> tryWithObservationSuspending(
    name: String,
    registry: ObservationRegistry,
    crossinline block: suspend () -> T,
): Result<T> =
    runCatching {
        withObservationSuspending(name, registry, block) ?: throw NoSuchElementException()
    }

/**
 * Suspend 함수 실행 시 Micrometer Observation을 이용하여 관찰(Observe)할 수 있도록 합니다.
 * Coroutine 컨텍스트와 Observation 컨텍스트 간의 전파를 자동으로 처리합니다.
 *
 * ```kotlin
 * withObservationContext("observer.delay", registry) {
 *     val observation = currentObservationInContext()!!
 *
 *     // some suspend code to observe
 *     delay(100.milliseconds)
 * }
 * ```
 *
 * @param T 반환 타입
 * @param name Micrometer Observation 이름
 * @param observationRegistry Observation을 등록할 [ObservationRegistry] 인스턴스
 * @param block Observation으로 관찰할 suspend 코드 블록
 * @return block의 실행 결과 또는 null
 */
suspend fun <T: Any> withObservationContextSuspending(
    name: String,
    observationRegistry: ObservationRegistry,
    block: suspend CoroutineScope.() -> T?,
): T? {
    name.requireNotBlank("name")
    val observation = observationRegistry.start(name)

    return try {
        withContext(observation.asCoroutineObservationContext()) {
            block()
        }
    } catch (e: Throwable) {
        observation.error(e)
        throw e
    } finally {
        observation.stop()
    }
}


/**
 * Suspend 함수 실행 시 Micrometer Observation을 이용하여 관찰(Observe)할 수 있도록 합니다.
 * Coroutine 컨텍스트와 Observation 컨텍스트 간의 전파를 자동으로 처리합니다.
 *
 *
 * ```kotlin
 * observation.withObservationContextSuspending { context ->
 *     // some suspend code to observe
 *     delay(100.milliseconds)
 *
 *     context.put("user.id", userId)
 *     processUser(userId)
 * }
 *
 * @param T 반환 타입
 * @param block Observation으로 관찰할 suspend 코드 블록
 * @return block의 실행 결과 또는 null
 */
suspend fun <T: Any> Observation.withObservationContextSuspending(
    block: suspend (Observation.Context) -> T?,
): T? {
    val observation = this@withObservationContextSuspending
    observation.start()

    return try {
        withContext(observation.asCoroutineObservationContext()) {
            block(observation.context)
        }
    } catch (e: Throwable) {
        observation.error(e)
        throw e
    } finally {
        observation.stop()
    }
}
