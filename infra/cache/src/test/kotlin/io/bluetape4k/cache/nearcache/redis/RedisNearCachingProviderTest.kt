package io.bluetape4k.cache.nearcache.redis

import io.bluetape4k.cache.jcache.getConfiguration
import io.bluetape4k.cache.jcache.getOrCreate
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.cache.jcache.jcachingProvider
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.redisson.jcache.configuration.JCacheConfiguration
import org.redisson.jcache.configuration.RedissonConfiguration
import javax.cache.Caching
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration

@Execution(ExecutionMode.SAME_THREAD)
class RedisNearCachingProviderTest {

    companion object: KLogging() {
        private val redisson by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }
    }

    @Test
    fun `CachingProvider 를 이용하여 NearCache 생성하기`() {
        val provider = Caching.getCachingProvider(RedisNearCachingProvider::class.qualifiedName)
        provider.shouldNotBeNull()
        val manager = provider.cacheManager
        manager.shouldNotBeNull()
        manager shouldBeInstanceOf RedisNearCacheManager::class

        // Redis 용 NearCache 를 생성하기 위해
        val configuration = jcacheConfiguration<Any, Any> {
            setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
        }
        val redissonCfg = RedissonConfiguration.fromInstance(redisson, configuration) as RedissonConfiguration<Any, Any>
        val redisNearCacheCfg =
            RedisNearCacheConfig(
                redissonConfig = redissonCfg,
                kType = Any::class.java,
                vType = Any::class.java
            )
        // val cache = manager.createCache(UUID.randomUUID().toString(), redisNearCacheCfg)
        val cache = manager.getOrCreate(Base58.randomString(16), redisNearCacheCfg)
        cache.shouldNotBeNull()
        cache shouldBeInstanceOf NearCache::class

        cache.put("key", "value")
        cache["key"] shouldBeEqualTo "value"

        val config = cache.getConfiguration<Any, Any, JCacheConfiguration<Any, Any>>()
        log.debug { "config expiryPolicy=${config.expiryPolicy.expiryForCreation.durationAmount}" }
        config.shouldNotBeNull()
    }

    @Test
    fun `CachingProvider close 하기`() {
        val provider = jcachingProvider<RedisNearCachingProvider>()
        provider.shouldNotBeNull()
        val manager = provider.cacheManager
        manager.shouldNotBeNull()
        manager shouldBeInstanceOf RedisNearCacheManager::class

        // Redis 용 NearCache 를 생성하기 위해
        val configuration = jcacheConfiguration<Any, Any> { }
        val redissonCfg = RedissonConfiguration.fromInstance(redisson, configuration)
        val redisNearCacheCfg =
            RedisNearCacheConfig(
                redissonConfig = redissonCfg as? RedissonConfiguration<Any, Any>,
                kType = Any::class.java,
                vType = Any::class.java
            )
        val cache = manager.createCache(Base58.randomString(16), redisNearCacheCfg)
        // val cache = manager.getOrCreate<Any, Any>(UUID.randomUUID().toString(), redissonCfg)
        cache.shouldNotBeNull()
        cache shouldBeInstanceOf NearCache::class

        provider.close()

        manager.isClosed.shouldBeTrue()
        redisson.isShutdown.shouldBeFalse() // Manager 내에서 관리하는 객체가 아니고, NearCache에서 관리하는 것이다
    }
}
