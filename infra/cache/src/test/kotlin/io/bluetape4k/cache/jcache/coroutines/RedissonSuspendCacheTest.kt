package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import java.util.*
import javax.cache.configuration.MutableConfiguration

class RedissonSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLoggingChannel() {
        val redisson by lazy { RedisServer.Launcher.RedissonLib.getRedisson() }
    }

    override val suspendCache: SuspendCache<String, Any> by lazy {
        RedissonSuspendCache(
            "coroutine-cache-" + UUID.randomUUID().encodeBase62(),
            redisson,
            MutableConfiguration()
        )
    }
}
