package io.bluetape4k.bucket4j

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import org.redisson.api.RedissonClient

object TestRedisServer: KLogging() {

    private val redis by lazy {
        RedisServer.Launcher.redis
    }

    fun lettuceClient(): RedisClient {
        return RedisServer.Launcher.LettuceLib.getRedisClient(redis.url)
    }

    fun redissonClient(): RedissonClient {
        return RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
    }
}
