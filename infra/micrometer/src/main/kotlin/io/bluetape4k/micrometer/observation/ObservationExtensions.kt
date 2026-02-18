package io.bluetape4k.micrometer.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry

/**
 * Observation을 시작하고 [block]을 실행한 후 Observation을 중지합니다.
 *
 * ```kotlin
 * val observation = registry.start("my.observation")
 * val result = observation.observe {
 *     someOperation()
 * }
 * ```
 *
 * @param T 반환 타입
 * @param block 측정할 코드 블록
 * @return block의 실행 결과 [Result]
 */
inline fun <T> Observation.tryObserve(
    @BuilderInference crossinline block: () -> T,
): Result<T> =
    runCatching {
        withObservationContext { _: Observation.Context ->
            block()
        }
    }

/**
 * [name]을 가진 Observation을 생성하고, [block]을 실행합니다.
 *
 * Observation을 사용하여 코드 블록의 실행을 추적하고, 성능 메트릭과 분산 추적 정보를 수집합니다.
 *
 * ```kotlin
 * val result = withObservation("user.service.getUser", registry) {
 *     userService.findById(userId)
 * }
 * ```
 *
 * @param T 반환 타입
 * @param name Observation의 이름
 * @param registry [ObservationRegistry]
 * @param block 측정할 코드 블록
 * @return block의 실행 결과
 */
inline fun <T> withObservation(
    name: String,
    registry: ObservationRegistry,
    @BuilderInference crossinline block: () -> T,
): T =
    Observation
        .createNotStarted(name, registry)
        .observe<T> { block() }

/**
 * Observation 컨텍스트와 함께 [block]을 실행합니다.
 * Observation의 생명주기(시작, 스코프 열기, 예외 처리, 중지)를 자동으로 관리합니다.
 *
 * ```kotlin
 * observation.withObservationContext { context ->
 *     context.put("user.id", userId)
 *     processUser(userId)
 * }
 * ```
 *
 * @param T 반환 타입
 * @param block Observation 컨텍스트를 받아 실행할 코드 블록
 * @return block의 실행 결과
 */
inline fun <T> Observation.withObservationContext(
    @BuilderInference block: (Observation.Context) -> T,
): T {
    val self = this
    start()
    return try {
        openScope().use { _ ->
            block(context)
        }
    } catch (e: Throwable) {
        self.error(e)
        throw e
    } finally {
        self.stop()
    }
}
