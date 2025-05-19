package io.bluetape4k.resilience4j.cache

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
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

    override val jcache: NearCache<String, String> by lazy {
        val nearCacheCfg = NearCacheConfig<String, String>()
        val backCache = JCaching.Redisson.getOrCreate<String, String>("back-coroutines", redisson)
        NearCache(nearCacheCfg, backCache)
    }

    @BeforeEach
    override fun setup() {
        jcache.clearAllCache()
        super.setup()
    }
}
