package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLoggingChannel()

    private val manager by lazy {
        LettuceSuspendCacheManager(redisClient, null, LettuceBinaryCodecs.lz4Fory())
    }

    override val suspendCache: SuspendCache<String, Any> =
        manager.getOrCreate("lettuce-suspend-cache-" + UUID.randomUUID().encodeBase62())


    @Test
    fun `ttl is refreshed for putAll putIfAbsent and replace paths`() = runSuspendIO {
        val ttlManager = LettuceSuspendCacheManager(redisClient, 1, LettuceBinaryCodecs.lz4Fory())
        val ttlCache = ttlManager.getOrCreate<String>("lettuce-suspend-ttl-" + UUID.randomUUID().encodeBase62())

        try {
            ttlCache.putAll(mapOf("k1" to "v1", "k2" to "v2"))
            await.atMost(3, TimeUnit.SECONDS) untilSuspending {
                (ttlCache.commands.ttl(ttlCache.cacheName) ?: -1L) > 0L
            }

            ttlCache.putIfAbsent("k3", "v3").shouldBeTrue()
            ttlCache.get("k3") shouldBeEqualTo "v3"
            await.atMost(3, TimeUnit.SECONDS) untilSuspending {
                (ttlCache.commands.ttl(ttlCache.cacheName) ?: -1L) > 0L
            }

            ttlCache.put("k4", "v4")
            ttlCache.replace("k4", "v4-updated").shouldBeTrue()
            await.atMost(3, TimeUnit.SECONDS) untilSuspending {
                (ttlCache.commands.ttl(ttlCache.cacheName) ?: -1L) > 0L
            }

            ttlCache.put("k5", "v5")
            ttlCache.replace("k5", "v5", "v5-updated").shouldBeTrue()
            await.atMost(3, TimeUnit.SECONDS) untilSuspending {
                (ttlCache.commands.ttl(ttlCache.cacheName) ?: -1L) > 0L
            }
        } finally {
            runCatching { ttlManager.close() }
        }
    }
}
