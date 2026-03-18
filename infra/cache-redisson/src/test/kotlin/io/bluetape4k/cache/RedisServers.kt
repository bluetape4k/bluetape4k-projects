package io.bluetape4k.cache

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.redis.redisson.redissonClientOf
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient

object RedisServers {
    /** 테스트 전역 Redis 서버 (Testcontainers) */
    val redisServer: RedisServer by lazy { RedisServer.Launcher.redis }

    @JvmStatic
    val redissonClient by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson(
            redisServer.url,
            256,
            24
        )
    }

    val redisson by lazy { redissonClient }

    internal fun newRedisson(): RedissonClient {
        val config =
            RedisServer.Launcher.RedissonLib.getRedissonConfig(
                redisServer.url,
                256,
                24
            )
        return redissonClientOf(config)
    }

    @JvmStatic
    val faker = Fakers.faker

    @JvmStatic
    internal fun randomName(): String = "$LibraryName:${Base58.randomString(8)}"

    @JvmStatic
    internal fun randomString(size: Int = 2048): String = Fakers.fixedString(size)
}
