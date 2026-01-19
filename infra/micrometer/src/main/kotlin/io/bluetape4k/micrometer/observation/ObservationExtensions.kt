package io.bluetape4k.micrometer.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry

/**
 * [name]을 가진 Observation을 생성하고, [block]을 실행합니다.
 *
 * @param T
 * @param name Observation의 이름
 * @param registry [ObservationRegistry]
 * @param block 측정할 코드 블록
 * @receiver
 * @return
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> withObservation(
    name: String,
    registry: ObservationRegistry,
    crossinline block: () -> T,
): T =
    Observation
        .createNotStarted(name, registry)
        .observe<T> {
            block()
        }

inline fun <T> Observation.observe(block: () -> T): T {
    return withObservationContext { _: Observation.Context ->
        block()
    }
}

inline fun <T> Observation.withObservationContext(block: (Observation.Context) -> T): T {
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
