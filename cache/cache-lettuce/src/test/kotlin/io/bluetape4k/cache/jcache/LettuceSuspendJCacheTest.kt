package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs

class LettuceSuspendJCacheTest: AbstractSuspendJCacheTest() {

    companion object: KLoggingChannel()

    private val manager by lazy {
        LettuceSuspendCacheManager(redisClient, null, LettuceBinaryCodecs.default())
    }

    override val suspendJCache: SuspendJCache<String, Any> =
        manager.getOrCreate("lettuce-suspend-cache-" + Base58.randomString(8))

}
