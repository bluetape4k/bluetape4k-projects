package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import java.time.Duration

/**
 * [ResilientSuspendNearCacheDecorator]를 통한 [SuspendNearCacheOperations] 통합 테스트 추상 클래스.
 *
 * 하위 클래스에서 [createBaseCache]로 실제 백엔드 SuspendNearCache를 생성하면,
 * `.withResilience {}` 로 래핑한 캐시에 대해 [AbstractSuspendNearCacheOperationsTest]의
 * 모든 테스트(CRUD + 동시성)를 실행합니다.
 *
 * ```kotlin
 * class ResilientLettuceSuspendNearCacheTest : AbstractResilientSuspendNearCacheOperationsTest<String>() {
 *     override fun createBaseCache() = lettuceSuspendNearCacheOf<String>(redisClient, codec, config)
 *     override fun sampleValue() = "hello"
 *     override fun anotherValue() = "world"
 * }
 * ```
 */
abstract class AbstractResilientSuspendNearCacheOperationsTest<V: Any> : AbstractSuspendNearCacheOperationsTest<V>() {

    companion object: KLogging()

    /**
     * Resilience로 래핑하기 전의 원본 SuspendNearCache를 생성합니다.
     */
    abstract fun createBaseCache(): SuspendNearCacheOperations<V>

    /**
     * Resilience 설정을 커스터마이징합니다.
     */
    open fun resilienceConfig(): NearCacheResilienceConfig = NearCacheResilienceConfig(
        retryMaxAttempts = 3,
        retryWaitDuration = Duration.ofMillis(100),
        retryExponentialBackoff = false,
        getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION,
    )

    override fun createCache(): SuspendNearCacheOperations<V> =
        createBaseCache().withResilience(resilienceConfig())
}
