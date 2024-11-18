package io.bluetape4k.cache.nearcache.redis

import io.bluetape4k.cache.jcache.getConfiguration
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.cache.jcache.jcachingProvider
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.redisson.jcache.configuration.JCacheConfiguration
import org.redisson.jcache.configuration.RedissonConfiguration
import java.util.*
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration

@Execution(ExecutionMode.SAME_THREAD)
class RedisNearCacheManagerTest {

    companion object: KLogging() {
        private val redisson by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }
    }

    // Redis 용 NearCache 를 생성하기 위해
    private val redissonCfg by lazy {
        val configuration = jcacheConfiguration<Any, Any> { }
        RedissonConfiguration.fromInstance(redisson, configuration)
    }
    private val redisNearCacheCfg by lazy {
        RedisNearCacheConfig(
            redissonConfig = redissonCfg as? RedissonConfiguration<Any, Any>,
            kType = Any::class.java,
            vType = Any::class.java
        )
    }

    @Test
    fun `NearCacheManager를 이용하여 NearCache 생성하기`() {
        val nearCacheManager = jcachingProvider<RedisNearCachingProvider>().cacheManager
        nearCacheManager shouldBeInstanceOf RedisNearCacheManager::class

        val cache = nearCacheManager.createCache(UUID.randomUUID().toString(), redisNearCacheCfg)
        cache shouldBeInstanceOf NearCache::class

        val nearCache = cache as NearCache<Any, Any>

        nearCache.put("key1", "value1")
        nearCache.putIfAbsent("key1", "new value 1").shouldBeFalse()

        nearCache["key1"] shouldBeEqualTo "value1"

        nearCache.close()
    }

    @Test
    fun `cache 에 다양한 설정 적용이 되어야 합니다`() {
        val nearCacheManager = jcachingProvider<RedisNearCachingProvider>().cacheManager

        val frontCacheConfiguration = jcacheConfiguration<String, String> {
            setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES))
        }
        val backCacheConfiguration = jcacheConfiguration<String, String> {
            setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES))
        }
        val nearCacheConfig = RedisNearCacheConfig(
            frontCacheConfiguration = frontCacheConfiguration,
            redissonConfig = RedissonConfiguration.fromInstance(
                redisson,
                backCacheConfiguration
            ) as RedissonConfiguration,
            kType = String::class.java,
            vType = String::class.java,
            checkExpiryPeriod = 0L          // 0 이면 검사하지 않습니다.
        )

        val cache = nearCacheManager.createCache("redis-near-cfg", nearCacheConfig)
        cache.shouldNotBeNull()
        cache.put("key-cfg-1", "cfg-1")
        cache.put("key-cfg-2", "cfg-2")
        cache["key-cfg-1"] shouldBeEqualTo "cfg-1"

        val backCacheCfg = cache.getConfiguration<String, String, JCacheConfiguration<String, String>>()
        backCacheCfg.shouldNotBeNull()
        backCacheCfg.expiryPolicy.expiryForCreation.durationAmount shouldBeEqualTo 30L
    }
}
