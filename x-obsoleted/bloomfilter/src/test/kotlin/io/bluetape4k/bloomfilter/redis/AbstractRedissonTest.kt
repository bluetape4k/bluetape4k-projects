package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient

abstract class AbstractRedissonTest {

    companion object: KLogging() {
        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }
    }

    protected val redisson: RedissonClient
        get() = RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
}
