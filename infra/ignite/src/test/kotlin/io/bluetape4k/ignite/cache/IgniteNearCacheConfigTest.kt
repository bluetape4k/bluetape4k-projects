package io.bluetape4k.ignite.cache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.ignite.cache.CacheMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [IgniteNearCacheConfig]의 기본값과 변환 메서드를 검증하는 테스트입니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteNearCacheConfigTest {

    @Test
    fun `기본값으로 생성된 IgniteNearCacheConfig 검증`() {
        val config = IgniteNearCacheConfig(cacheName = "TEST_CACHE")

        config.cacheName shouldBeEqualTo "TEST_CACHE"
        config.cacheMode shouldBeEqualTo CacheMode.PARTITIONED
        config.nearMaxSize shouldBeEqualTo 10_000
        config.frontCacheMaxSize shouldBeEqualTo 10_000L
        config.frontCacheTtlSeconds shouldBeEqualTo 600L
    }

    @Test
    fun `readOnly 팩토리로 생성한 설정이 REPLICATED 모드를 가짐`() {
        val config = IgniteNearCacheConfig.readOnly("RO_CACHE")

        config.cacheName shouldBeEqualTo "RO_CACHE"
        config.cacheMode shouldBeEqualTo CacheMode.REPLICATED
        config.frontCacheTtlSeconds shouldBeEqualTo 3600L
    }

    @Test
    fun `toNearCacheConfiguration 변환 검증`() {
        val config = IgniteNearCacheConfig(
            cacheName = "NEAR_CACHE",
            nearMaxSize = 500,
        )

        val nearCacheCfg = config.toNearCacheConfiguration<String, String>()

        nearCacheCfg.shouldNotBeNull()
        nearCacheCfg.nearEvictionPolicyFactory.shouldNotBeNull()
    }

    @Test
    fun `toCacheConfiguration 변환 시 캐시 이름과 모드가 올바르게 설정됨`() {
        val config = IgniteNearCacheConfig(
            cacheName = "CACHE_CFG",
            cacheMode = CacheMode.REPLICATED,
        )

        val cacheCfg = config.toCacheConfiguration<String, String>()

        cacheCfg.shouldNotBeNull()
        cacheCfg.name shouldBeEqualTo "CACHE_CFG"
        cacheCfg.cacheMode shouldBeEqualTo CacheMode.REPLICATED
    }

    @Test
    fun `기본 모드로 생성한 CacheConfiguration은 PARTITIONED 모드`() {
        val config = IgniteNearCacheConfig(cacheName = "PART_CACHE")
        val cacheCfg = config.toCacheConfiguration<String, String>()

        cacheCfg.cacheMode shouldBeEqualTo CacheMode.PARTITIONED
    }
}
