package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import java.time.Duration

/**
 * [ResilientNearCacheDecorator]를 통한 [NearCacheOperations] 통합 테스트 추상 클래스.
 *
 * 하위 클래스에서 [createBaseCache]로 실제 백엔드 NearCache를 생성하면,
 * `.withResilience {}` 로 래핑한 캐시에 대해 [AbstractNearCacheOperationsTest]의
 * 모든 테스트(CRUD + 동시성)를 실행합니다.
 *
 * ```kotlin
 * class ResilientLettuceNearCacheTest : AbstractResilientNearCacheOperationsTest<String>() {
 *     override fun createBaseCache() = lettuceNearCacheOf<String>(redisClient, codec, config)
 *     override fun sampleValue() = "hello"
 *     override fun anotherValue() = "world"
 * }
 * ```
 */
abstract class AbstractResilientNearCacheOperationsTest<V: Any> : AbstractNearCacheOperationsTest<V>() {

    companion object: KLogging()

    /**
     * Resilience로 래핑하기 전의 원본 NearCache를 생성합니다.
     */
    abstract fun createBaseCache(): NearCacheOperations<V>

    /**
     * Resilience 설정을 커스터마이징합니다. 기본값은 retryMaxAttempts=3, 100ms 대기.
     */
    open fun resilienceConfig(): NearCacheResilienceConfig = NearCacheResilienceConfig(
        retryMaxAttempts = 3,
        retryWaitDuration = Duration.ofMillis(100),
        retryExponentialBackoff = false,
        getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION,
    )

    override fun createCache(): NearCacheOperations<V> =
        createBaseCache().withResilience(resilienceConfig())
}
