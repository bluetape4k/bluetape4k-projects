package io.bluetape4k.micrometer.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry

/**
 * 아무 동작도 수행하지 않는 NOOP [ObservationRegistry]를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `ObservationRegistry.NOOP` 상수를 그대로 반환합니다.
 * - 생성 비용이나 추가 할당이 없습니다.
 *
 * ```kotlin
 * val registry = NoopObservationRegistry
 * // registry === ObservationRegistry.NOOP
 * ```
 */
val NoopObservationRegistry: ObservationRegistry get() = ObservationRegistry.NOOP

/**
 * 관측 컨텍스트 처리 함수를 등록한 [ObservationRegistry]를 생성합니다.
 *
 * ## 동작/계약
 * - `ObservationRegistry.create()`로 새 레지스트리를 만들고 handler를 1개 등록합니다.
 * - [observationHandler]가 `true`를 반환하면 체인 처리를 계속합니다.
 * - handler를 명시하지 않으면 모든 컨텍스트를 통과시키는 기본 구현을 사용합니다.
 *
 * ```kotlin
 * val registry = observationRegistryOf { ctx -> ctx.name != null }
 * // registry != ObservationRegistry.NOOP
 * ```
 */
inline fun observationRegistryOf(crossinline observationHandler: (Observation.Context) -> Boolean = { true }): ObservationRegistry =
    ObservationRegistry.create().apply {
        this.observationConfig().observationHandler { observationHandler(it) }
    }

/**
 * 단순 처리 함수 기반 [ObservationRegistry]를 생성합니다.
 *
 * ## 동작/계약
 * - [observationHandler]를 실행한 뒤 항상 `true`를 반환해 체인 처리를 유지합니다.
 * - 반환값 없는 부수효과 처리(handler 로깅/태깅)에 적합합니다.
 * - 호출마다 새 레지스트리 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val registry = simpleObservationRegistryOf { ctx -> ctx.put("service", "user") }
 * // registry != ObservationRegistry.NOOP
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
 * 이름과 컨텍스트 이름을 설정한 [Observation]을 생성하고 즉시 시작합니다.
 *
 * ## 동작/계약
 * - `Observation.Context`에 `name`, `contextualName` 값을 저장합니다.
 * - `Observation.start(...)`를 호출해 시작된 인스턴스를 반환합니다.
 * - [contextualName] 기본값은 [name]입니다.
 *
 * ```kotlin
 * val obs = registry.start("user.service", "getUser")
 * obs.stop()
 * // obs.context.get<String>("name") == "user.service"
 * ```
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
 * 이름과 컨텍스트를 설정한 [Observation]을 생성하되 시작하지 않고 반환합니다.
 *
 * ## 동작/계약
 * - `Observation.Context`에 `name`, `contextualName`를 저장합니다.
 * - 반환 후 호출자가 key-value를 추가한 뒤 `.start()`를 호출할 수 있습니다.
 * - [contextualName] 기본값은 [name]입니다.
 *
 * ```kotlin
 * val obs = registry.createNotStarted("user.service")
 * obs.start(); obs.stop()
 * // obs.context.get<String>("contextualName") == "user.service"
 * ```
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
