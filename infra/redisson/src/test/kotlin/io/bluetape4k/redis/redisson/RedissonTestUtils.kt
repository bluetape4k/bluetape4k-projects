package io.bluetape4k.redis.redisson

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient

object RedissonTestUtils {

    @JvmStatic
    internal val redisServer by lazy { RedisServer.Launcher.redis }

    @JvmStatic
    val redissonClient by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson(
            redisServer.url,
            256,
            24
        )
    }

    internal val redisson: RedissonClient get() = redissonClient

    internal fun newRedisson(): RedissonClient {
        val config = RedisServer.Launcher.RedissonLib.getRedissonConfig(redisServer.url)
        return redissonClientOf(config)
    }

    @JvmStatic
    val faker = Fakers.faker

    @JvmStatic
    internal fun randomName(): String =
        "$LibraryName:${Base58.randomString(8)}"

    @JvmStatic
    internal fun randomString(size: Int = 2048): String =
        Fakers.fixedString(size)
}
