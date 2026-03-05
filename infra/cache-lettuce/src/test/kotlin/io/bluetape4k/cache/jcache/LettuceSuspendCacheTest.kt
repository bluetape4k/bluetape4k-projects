package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import java.util.*

class LettuceSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLoggingChannel()

    private val manager by lazy {
        LettuceSuspendCacheManager(redisClient, null, LettuceBinaryCodecs.lz4Fory())
    }

    override val suspendCache: SuspendCache<String, Any> =
        manager.getOrCreate("lettuce-suspend-cache-" + UUID.randomUUID().encodeBase62())
}
