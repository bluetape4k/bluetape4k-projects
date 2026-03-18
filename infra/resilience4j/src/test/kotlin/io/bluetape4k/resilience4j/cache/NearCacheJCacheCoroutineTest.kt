package io.bluetape4k.resilience4j.cache

import io.bluetape4k.cache.jcache.RedissonJCaching
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import org.junit.jupiter.api.BeforeEach
import org.redisson.api.RedissonClient

class NearCacheJCacheCoroutineTest: AbstractJCacheCoroutinesTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        val redisson: RedissonClient by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }
    }

    override val jcache: NearJCache<String, String> by lazy {
        val nearCacheCfg = NearJCacheConfig<String, String>()
        val backCache = RedissonJCaching.getOrCreate<String, String>("back-coroutines", redisson)
        NearJCache(nearCacheCfg, backCache)
    }

    @BeforeEach
    override fun setup() {
        jcache.clearAllCache()
        super.setup()
    }
}
