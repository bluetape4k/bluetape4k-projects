package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import java.util.*
import javax.cache.configuration.MutableConfiguration

class RedissonCoCacheTest: AbstractCoCacheTest() {

    companion object: KLoggingChannel() {
        val redisson by lazy { RedisServer.Launcher.RedissonLib.getRedisson() }
    }

    override val coCache: CoCache<String, Any> by lazy {
        RedissonCoCache(
            "coroutine-cache-" + UUID.randomUUID().encodeBase62(),
            redisson,
            MutableConfiguration()
        )
    }
}
