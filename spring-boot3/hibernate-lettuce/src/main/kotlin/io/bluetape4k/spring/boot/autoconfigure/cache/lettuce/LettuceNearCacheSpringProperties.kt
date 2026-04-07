package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * `application.yml` / `application.properties` 기반 Spring Boot 설정.
 *
 * ```kotlin
 * // application.yml
 * // bluetape4k.cache.lettuce-near.redis-uri: redis://localhost:6379
 * // bluetape4k.cache.lettuce-near.codec: zstdfory
 * // bluetape4k.cache.lettuce-near.use-resp3: true
 * // bluetape4k.cache.lettuce-near.local.max-size: 10000
 * // bluetape4k.cache.lettuce-near.local.expire-after-write: 30m
 * // bluetape4k.cache.lettuce-near.redis-ttl.default: 120s
 * // bluetape4k.cache.lettuce-near.redis-ttl.regions.myEntity: 300s
 * // bluetape4k.cache.lettuce-near.metrics.enabled: true
 * // bluetape4k.cache.lettuce-near.metrics.enable-caffeine-stats: true
 * ```
 */
@ConfigurationProperties(prefix = "bluetape4k.cache.lettuce-near")
data class LettuceNearCacheSpringProperties(
    val enabled: Boolean = true,
    val redisUri: String = "redis://localhost:6379",
    val codec: String = "lz4fory",
    val useResp3: Boolean = true,
    val local: LocalProperties = LocalProperties(),
    val redisTtl: RedisTtlProperties = RedisTtlProperties(),
    val metrics: MetricsProperties = MetricsProperties(),
) {
    /**
     * Caffeine L1 캐시의 최대 항목 수와 만료 정책이다.
     *
     * `maxSize`는 항목 개수 기준이며, `expireAfterWrite`는 마지막 쓰기 이후 TTL이다.
     */
    data class LocalProperties(
        val maxSize: Long = 10_000,
        val expireAfterWrite: Duration = Duration.ofMinutes(30),
    )

    /**
     * Redis L2 캐시의 TTL 설정이다.
     *
     * `default`가 기본값이고, `regions`가 동일 region 이름에 대해 우선 적용된다.
     */
    data class RedisTtlProperties(
        val default: Duration = Duration.ofSeconds(120),
        val regions: Map<String, Duration> = emptyMap(),
    )

    /**
     * Micrometer/Actuator 연동 여부를 제어한다.
     *
     * `enableCaffeineStats=true`이면 Hibernate statistics와 함께 Caffeine local stats도 기록한다.
     */
    data class MetricsProperties(
        val enabled: Boolean = true,
        val enableCaffeineStats: Boolean = true,
    )
}
