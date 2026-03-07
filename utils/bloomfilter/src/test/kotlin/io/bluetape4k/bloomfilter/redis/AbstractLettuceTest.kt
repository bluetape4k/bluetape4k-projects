package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

abstract class AbstractLettuceTest {

    companion object: KLogging() {
        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redisClient: RedisClient by lazy {
            RedisServer.Launcher.LettuceLib.getRedisClient(redis.host, redis.port)
        }
    }

    protected val connection: StatefulRedisConnection<String, String> by lazy {
        redisClient.connect()
    }
}
