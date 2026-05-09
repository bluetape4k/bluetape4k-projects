package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.*
import javax.cache.configuration.MutableConfiguration

class RedissonSuspendJCacheTest: AbstractSuspendJCacheTest() {

    companion object: KLoggingChannel()

    override val suspendJCache: SuspendJCache<String, Any> =
        RedissonSuspendJCache(
            "redis-suspend-cache-" + UUID.randomUUID().encodeBase62(),
            RedisServers.redisson,
            MutableConfiguration()
        )
}
