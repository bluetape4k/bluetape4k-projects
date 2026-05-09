package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * NearCache Resilience Decorator 설정.
 *
 * [ResilientNearCacheDecorator] 및 [ResilientSuspendNearCacheDecorator]에서 사용하는
 * retry 및 failure strategy 설정입니다.
 *
 * @property retryMaxAttempts 최대 재시도 횟수
 * @property retryWaitDuration 재시도 대기 시간. 0보다 커야 한다.
 * @property retryExponentialBackoff 지수 백오프 사용 여부
 * @property getFailureStrategy back cache GET 실패 시 동작 전략
 */
data class NearCacheResilienceConfig(
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = Duration.ofMillis(500),
    val retryExponentialBackoff: Boolean = true,
    val getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL,
) {
    init {
        retryMaxAttempts.requirePositiveNumber("retryMaxAttempts")
        retryWaitDuration.requireGt(Duration.ZERO, "retryWaitDuration")
    }
}

/**
 * [NearCacheResilienceConfig] DSL 빌더 함수.
 *
 * ```kotlin
 * val config = nearCacheResilienceConfig {
 *     retryMaxAttempts = 5
 *     retryWaitDuration = Duration.ofSeconds(1)
 *     getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
 * }
 * ```
 */
inline fun nearCacheResilienceConfig(block: NearCacheResilienceConfigBuilder.() -> Unit): NearCacheResilienceConfig =
    NearCacheResilienceConfigBuilder().apply(block).build()

/**
 * [NearCacheResilienceConfig] 빌더 클래스.
 */
class NearCacheResilienceConfigBuilder {
    var retryMaxAttempts: Int = 3
    var retryWaitDuration: Duration = Duration.ofMillis(500)
    var retryExponentialBackoff: Boolean = true
    var getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL

    fun build(): NearCacheResilienceConfig =
        NearCacheResilienceConfig(
            retryMaxAttempts = retryMaxAttempts.requirePositiveNumber("retryMaxAttempts"),
            retryWaitDuration = retryWaitDuration,
            retryExponentialBackoff = retryExponentialBackoff,
            getFailureStrategy = getFailureStrategy
        )
}
