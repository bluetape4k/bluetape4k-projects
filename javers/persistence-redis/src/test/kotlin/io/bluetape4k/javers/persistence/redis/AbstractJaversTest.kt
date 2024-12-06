package io.bluetape4k.javers.persistence.redis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer

abstract class AbstractJaversTest {

    companion object: KLogging() {
        val redis = RedisServer.Launcher.redis
    }

    protected val lettuceClient: io.lettuce.core.RedisClient by lazy {
        RedisServer.Launcher.LettuceLib.getRedisClient(redis.host, redis.port)
    }

    protected val redisson by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
    }

}
