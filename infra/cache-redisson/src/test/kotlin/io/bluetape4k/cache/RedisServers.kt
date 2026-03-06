package io.bluetape4k.cache

import io.bluetape4k.testcontainers.storage.RedisServer

object RedisServers {

    /** 테스트 전역 Redis 서버 (Testcontainers) */
    val redis: RedisServer by lazy { RedisServer.Launcher.redis }

    val redisson by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
    }

    /**
     * RESP3 를 사용하기 위한 Lettuce 의 RedisClient
     */
    val redisClient by lazy { RedisServer.Launcher.LettuceLib.getRedisClient(redis.url) }
}
