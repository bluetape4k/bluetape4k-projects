package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.testcontainers.storage.RedisServer
import java.util.*

class LettuceSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLoggingChannel() {
        val redisClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient() }
    }

    private val manager by lazy {
        LettuceSuspendCacheManager(redisClient, null, LettuceBinaryCodecs.lz4Fory())
    }

    override val suspendCache: SuspendCache<String, Any> =
        manager.getOrCreate("lettuce-suspend-cache-" + UUID.randomUUID().encodeBase62())
}
