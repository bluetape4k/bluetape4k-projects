package io.bluetape4k.resilience4j.cache

import io.bluetape4k.cache.jcache.RedissonJCaching
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient
import javax.cache.Cache

class RedissonJCacheCoroutineTest: AbstractJCacheCoroutinesTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        val redisson: RedissonClient by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }
    }

    override val jcache: Cache<String, String> by lazy {
        RedissonJCaching.getOrCreate("redisson.coroutines", redisson)
    }
}
