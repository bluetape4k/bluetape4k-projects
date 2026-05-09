package io.bluetape4k.cache

import io.bluetape4k.testcontainers.storage.RedisServer

object RedisServers {

    /** 테스트 전역 Redis 서버 (Testcontainers) */
    val redis: RedisServer by lazy { RedisServer.Launcher.redis }

    val redisClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient(redis.url) }
}
