package io.bluetape4k.micrometer.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry

val NoopObservationRegistry: ObservationRegistry get() = ObservationRegistry.NOOP

/**
 * [ObservationRegistry] with a custom [io.micrometer.observation.ObservationHandler].
 *
 * ObservationHandler를 등록하지 않으면, 기본적으로 [io.micrometer.observation.Observation.NOOP]으로 취급해서, 아무런 동작을 하지 않습니다.
 *
 * ```
 * val observationRegistry = observationRegistryOf { ctx ->
 *         log.trace { "Current context: $ctx" }
 *         true
 * }
 * ```
 */
inline fun observationRegistryOf(
    crossinline observationHandler: (Observation.Context) -> Boolean = { true },
): ObservationRegistry {
    return ObservationRegistry.create().apply {
        this.observationConfig().observationHandler { observationHandler(it) }
    }
}

/**
 * [io.micrometer.observation.SimpleObservationRegistry] with a custom [io.micrometer.observation.ObservationHandler].
 *
 * ObservationHandler를 등록하지 않으면, 기본적으로 [io.micrometer.observation.Observation.NOOP]으로 취급해서, 아무런 동작을 하지 않습니다.
 *
 * ```
 * val observationRegistry = simpleObservationRegistryOf { ctx ->
 *         log.trace { "Current context: $ctx" }
 * }
 * ```
 */
inline fun simpleObservationRegistryOf(
    crossinline observationHandler: (Observation.Context) -> Unit = { },
): ObservationRegistry {
    return ObservationRegistry.create().apply {
        this.observationConfig().observationHandler {
            observationHandler(it)
            true
        }
    }
}

fun ObservationRegistry.start(
    name: String,
    contextualName: String = name,
): Observation {
    val context = Observation.Context().apply {
        put("name", name)
        put("contextualName", contextualName)
    }
    return Observation.start(name, { context }, this)
}

fun ObservationRegistry.createNotStarted(
    name: String,
    contextualName: String = name,
): Observation {
    val context = Observation.Context().apply {
        put("name", name)
        put("contextualName", contextualName)
    }

    return Observation.createNotStarted(
        name,
        { context },
        this
    )
}
