package io.bluetape4k.exposed.cache.redis

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Duration

/**
 * Redis 기반 캐시 저장소의 Resilience 설정.
 *
 * Lettuce/Redisson Redis 저장소에서 선택적으로 활성화합니다.
 * `null`이면 Resilience 비활성화 (기존 동작 유지).
 *
 * ```kotlin
 * val resilience = RedisRepositoryResilienceConfig(
 *     retryMaxAttempts = 5,
 *     circuitBreakerEnabled = true,
 * )
 * ```
 *
 * @property retryMaxAttempts 최대 재시도 횟수 (기본값: 3)
 * @property retryWaitDuration 재시도 대기 시간 (기본값: 500ms)
 * @property retryExponentialBackoff 지수 백오프 사용 여부 (기본값: true)
 * @property circuitBreakerEnabled Circuit Breaker 활성화 여부 (기본값: false)
 * @property timeoutDuration 타임아웃 시간 (기본값: 2초)
 */
data class RedisRepositoryResilienceConfig(
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = Duration.ofMillis(500),
    val retryExponentialBackoff: Boolean = true,
    val circuitBreakerEnabled: Boolean = false,
    val timeoutDuration: Duration = Duration.ofSeconds(2),
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
