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
 * Current Coroutine Scope에서 Observation을 가져옵니다. 없다면 null 반환
 *
 * ```
 * observationRegistry.withObserver("observer.delay") {
 *   val observation = currentObservation()!!
 *   // some code to observe
 *   delay(100.milliseconds)
 * }
 * ```
 *
 * @return
 */
suspend fun currentObservationInContext(): Observation? =
    currentReactiveContext()?.getOrNull(ObservationThreadLocalAccessor.KEY)

/**
 * Suspend 함수를 실행 시에 Micrometer Observation을 이용하여 Observe 할 수 있도록 합니다.
 *
 * ```
 * observationRegistry.withObservationContext("observer.delay") {
 *      val observation = currentObservation()!!
 *
 *      // some suspend code to observe
 *      delay(100.milliseconds)
 * }
 * ```
 *
 * @param T
 * @param name Micrometer Observation 이름
 * @param block Micrometer Observation으로 실행을 관측할 코드블럭
 * @receiver [ObservationRegistry] 인스턴스
 * @return 반환 값
 */
suspend inline fun <T: Any> withObservationContext(
    name: String,
    observationRegistry: ObservationRegistry,
    crossinline block: suspend CoroutineScope.() -> T?,
): T? = Mono.deferContextual { contextView ->
    name.requireNotBlank("name")
    val snapshotFactory = ContextSnapshotFactory.builder().build()
    snapshotFactory.setThreadLocalsFrom<T>(contextView, ObservationThreadLocalAccessor.KEY).use { _ ->
        val observation = observationRegistry.start(name)
        Mono.defer {
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
