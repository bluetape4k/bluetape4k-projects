package io.bluetape4k.micrometer.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry

/**
 * 아묟은 동작도 하지 않는 Noop ObservationRegistry 인스턴스입니다.
 */
val NoopObservationRegistry: ObservationRegistry get() = ObservationRegistry.NOOP

/**
 * 커스텀 [io.micrometer.observation.ObservationHandler]를 가진 [ObservationRegistry]를 생성합니다.
 *
 * ObservationHandler를 등록하지 않으면, 기본적으로 [io.micrometer.observation.Observation.NOOP]으로 취급해서, 아묟은 동작을 하지 않습니다.
 *
 * ```kotlin
 * val observationRegistry = observationRegistryOf { ctx ->
 *     log.trace { "Current context: $ctx" }
 *     true
 * }
 * ```
 *
 * @param observationHandler Observation 컨텍스트를 처리할 핸들러. true를 반환하면 계속 처리됩니다.
 * @return 설정된 [ObservationRegistry] 인스턴스
 */
inline fun observationRegistryOf(crossinline observationHandler: (Observation.Context) -> Boolean = { true }): ObservationRegistry =
    ObservationRegistry.create().apply {
        this.observationConfig().observationHandler { observationHandler(it) }
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
inline fun simpleObservationRegistryOf(crossinline observationHandler: (Observation.Context) -> Unit = { }): ObservationRegistry =
    ObservationRegistry.create().apply {
        this.observationConfig().observationHandler {
            observationHandler(it)
            true
        }
    }

/**
 * 주어진 이름으로 Observation을 시작합니다.
 *
 * ```kotlin
 * val observation = registry.start("user.service", "getUser")
 * try {
 *     val user = userService.findById(id)
 *     observation.stop()
 *     return user
 * } catch (e: Exception) {
 *     observation.error(e)
 *     throw e
 * }
 * ```
 *
 * @param name Observation의 이름
 * @param contextualName 컨텍스트 이름 (기본값: name)
 * @return 시작된 [Observation] 인스턴스
 */
fun ObservationRegistry.start(
    name: String,
    contextualName: String = name,
): Observation {
    val context =
        Observation.Context().apply {
            put("name", name)
            put("contextualName", contextualName)
        }
    return Observation.start(name, { context }, this)
}

/**
 * 주어진 이름으로 Observation을 생성하지만 시작하지는 않습니다.
 * 추가 설정을 한 후 [Observation.start()]를 호출해야 합니다.
 *
 * ```kotlin
 * val observation = registry.createNotStarted("user.service")
 *     .lowCardinalityKeyValue("user.type", "premium")
 *     .start()
 * ```
 *
 * @param name Observation의 이름
 * @param contextualName 컨텍스트 이름 (기본값: name)
 * @return 생성된 [Observation] 인스턴스
 */
fun ObservationRegistry.createNotStarted(
    name: String,
    contextualName: String = name,
): Observation {
    val context =
        Observation.Context().apply {
            put("name", name)
            put("contextualName", contextualName)
        }

    return Observation.createNotStarted(
        name,
        { context },
        this,
    )
}
