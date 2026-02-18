package io.bluetape4k.micrometer.observation.coroutines

import io.bluetape4k.coroutines.reactor.currentReactiveContext
import io.bluetape4k.coroutines.reactor.getOrNull
import io.bluetape4k.micrometer.observation.start
import io.bluetape4k.support.requireNotBlank
import io.micrometer.context.ContextSnapshotFactory
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import reactor.util.context.Context

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

suspend inline fun <T: Any> Observation.observeSuspending(
    @BuilderInference crossinline block: suspend (Observation.Context) -> T?,
): T? =
    withObservationContextSuspending { ctx: Observation.Context ->
        block(ctx)
    }

suspend inline fun <T: Any> Observation.tryObserveSuspending(
    @BuilderInference crossinline block: suspend (Observation.Context) -> T?,
): Result<T> =
    runCatching {
        withObservationContextSuspending { ctx: Observation.Context ->
            block(ctx)
        } ?: throw NoSuchElementException()
    }

suspend inline fun <T: Any> withObservationSuspending(
    name: String,
    registry: ObservationRegistry,
    @BuilderInference crossinline block: suspend () -> T?,
): T? =
    withObservationContextSuspending(name, registry) {
        block()
    }

suspend inline fun <T: Any> tryWithObservationSuspending(
    name: String,
    registry: ObservationRegistry,
    @BuilderInference crossinline block: suspend () -> T,
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
): T? =
    Mono
        .deferContextual { contextView ->
            name.requireNotBlank("name")
            val snapshotFactory = ContextSnapshotFactory.builder().build()
            snapshotFactory.setThreadLocalsFrom<T>(contextView, ObservationThreadLocalAccessor.KEY).use { _ ->
                val observation = observationRegistry.start(name)
                Mono
                    .defer {
                        // Tracing 정보를 보려면, 아래와 같이 TracingObservationHandler.TracingContext 에서 가져오면 된다.
                        //                val tracingContext = observation.context.get<TracingObservationHandler.TracingContext>(TracingObservationHandler.TracingContext::class.java)
                        //                log.info(
                        //                    "tracingContext traceId=${tracingContext?.span?.context()?.traceId()}, " +
                        //                        "spanId=${tracingContext?.span?.context()?.spanId()}"
                        //                )
                        mono(Context.of(ObservationThreadLocalAccessor.KEY, observation).asCoroutineContext()) {
                            observation.openScope().use {
                                block()
                            }
                        }
                    }.doOnError {
                        observation.error(it)
                    }.doFinally {
                        observation.stop()
                    }
            }
        }.awaitSingleOrNull()


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
): T? =
    Mono
        .deferContextual { contextView ->
            val snapshotFactory = ContextSnapshotFactory.builder().build()
            snapshotFactory.setThreadLocalsFrom<T>(contextView, ObservationThreadLocalAccessor.KEY).use { _ ->
                val observation = this@withObservationContextSuspending
                Mono
                    .defer {
                        // Tracing 정보를 보려면, 아래와 같이 TracingObservationHandler.TracingContext 에서 가져오면 된다.
                        //                val tracingContext = observation.context.get<TracingObservationHandler.TracingContext>(TracingObservationHandler.TracingContext::class.java)
                        //                log.info(
                        //                    "tracingContext traceId=${tracingContext?.span?.context()?.traceId()}, " +
                        //                        "spanId=${tracingContext?.span?.context()?.spanId()}"
                        //                )
                        mono(Context.of(ObservationThreadLocalAccessor.KEY, observation).asCoroutineContext()) {
                            observation.openScope().use {
                                block(observation.context)
                            }
                        }
                    }.doOnError {
                        observation.error(it)
                    }.doFinally {
                        observation.stop()
                    }
            }
        }.awaitSingleOrNull()
