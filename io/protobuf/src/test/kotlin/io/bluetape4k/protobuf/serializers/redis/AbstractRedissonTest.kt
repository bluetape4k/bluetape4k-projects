package io.bluetape4k.protobuf.serializers.redis

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.redissonClientOf
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient

abstract class AbstractRedissonTest {

    companion object: KLogging() {
        @JvmStatic
        protected val redisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redissonClient by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson(
                redisServer.url,
                256,
                24
            )
        }

        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        protected fun randomName(): String =
            "$LibraryName:${Base58.randomString(8)}"

        @JvmStatic
        protected fun randomString(size: Int = 2048): String =
            Fakers.fixedString(size)
    }

    protected val redisson: RedissonClient get() = redissonClient

    protected fun newRedisson(): RedissonClient {
        val config = RedisServer.Launcher.RedissonLib.getRedissonConfig(
            redisServer.url,
            256,
            24
        )
        return redissonClientOf(config)
    }


}
