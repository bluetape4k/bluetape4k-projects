package io.bluetape4k.spring.observability

import io.bluetape4k.support.requireNotBlank
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.withContext
import reactor.util.context.Context
import java.io.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Micrometer key values applied when a Spring Boot observation is created.
 *
 * ## Contract
 * - [lowCardinality] is suitable for metrics labels and bounded dimensions such as `component` or `outcome`.
 * - [highCardinality] is reserved for trace-only dimensions such as sanitized identifiers.
 * - No exporter, OpenTelemetry SDK, or Prometheus endpoint is configured by this value object.
 *
 * ```kotlin
 * val keyValues = SpringObservationKeyValues(
 *     lowCardinality = KeyValues.of(KeyValue.of("component", "order-service")),
 * )
 * ```
 */
class SpringObservationKeyValues(
    val lowCardinality: KeyValues = KeyValues.empty(),
    val highCardinality: KeyValues = KeyValues.empty(),
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Observe a Spring Boot service block with the application-owned [ObservationRegistry].
 *
 * ## Contract
 * - The observation is created from [registry][ObservationRegistry] and never mutates global OpenTelemetry state.
 * - The observation starts before [block] and stops after [block], including failure paths.
 * - [CancellationException] is rethrown without being recorded as an observation error.
 * - Non-cancellation exceptions are recorded on the observation and rethrown.
 *
 * ```kotlin
 * val result = observationRegistry.observeSpring(
 *     name = "order.service.load",
 *     keyValues = SpringObservationKeyValues(
 *         lowCardinality = KeyValues.of(KeyValue.of("component", "order-service")),
 *     ),
 * ) { context ->
 *     context.addLowCardinalityKeyValue(KeyValue.of("outcome", "success"))
 *     orderService.load(orderId)
 * }
 * ```
 */
fun <T> ObservationRegistry.observeSpring(
    name: String,
    keyValues: SpringObservationKeyValues = SpringObservationKeyValues(),
    block: (Observation.Context) -> T,
): T {
    name.requireNotBlank("name")

    val observation = Observation
        .createNotStarted(name, this)
        .applySpringKeyValues(keyValues)

    observation.start()
    return try {
        observation.openScope().use {
            block(observation.context)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        observation.error(e)
        throw e
    } finally {
        observation.stop()
    }
}

/**
 * Observe a suspend Spring Boot block with the application-owned [ObservationRegistry].
 *
 * ## Contract
 * - The observation is bound to Reactor and coroutine context so child suspensions can read the current observation.
 * - [CancellationException] is rethrown without being recorded as an observation error.
 * - Non-cancellation exceptions are recorded on the observation and rethrown.
 *
 * ```kotlin
 * val result = observationRegistry.observeSpringSuspending("order.handler") { context ->
 *     context.addLowCardinalityKeyValue(KeyValue.of("handler", "get-order"))
 *     orderService.load(orderId)
 * }
 * ```
 */
suspend fun <T> ObservationRegistry.observeSpringSuspending(
    name: String,
    keyValues: SpringObservationKeyValues = SpringObservationKeyValues(),
    block: suspend (Observation.Context) -> T,
): T {
    name.requireNotBlank("name")

    val observation = Observation
        .createNotStarted(name, this)
        .applySpringKeyValues(keyValues)

    observation.start()
    return try {
        withContext(observation.asSpringCoroutineObservationContext()) {
            block(observation.context)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        observation.error(e)
        throw e
    } finally {
        observation.stop()
    }
}

private fun Observation.applySpringKeyValues(keyValues: SpringObservationKeyValues): Observation =
    apply {
        keyValues.lowCardinality.forEach { keyValue: KeyValue ->
            lowCardinalityKeyValue(keyValue)
        }
        keyValues.highCardinality.forEach { keyValue: KeyValue ->
            highCardinalityKeyValue(keyValue)
        }
    }

private class SpringObservationScopeContextElement(
    private val observation: Observation,
): ThreadContextElement<Observation.Scope>, AbstractCoroutineContextElement(Key) {

    companion object Key: CoroutineContext.Key<SpringObservationScopeContextElement>

    override fun updateThreadContext(context: CoroutineContext): Observation.Scope =
        observation.openScope()

    override fun restoreThreadContext(context: CoroutineContext, oldState: Observation.Scope) {
        oldState.close()
    }
}

private suspend fun Observation.asSpringCoroutineObservationContext(): CoroutineContext {
    val reactorContext = (coroutineContext[ReactorContext]?.context ?: Context.empty())
        .put(ObservationThreadLocalAccessor.KEY, this)

    return reactorContext.asCoroutineContext() + SpringObservationScopeContextElement(this)
}
