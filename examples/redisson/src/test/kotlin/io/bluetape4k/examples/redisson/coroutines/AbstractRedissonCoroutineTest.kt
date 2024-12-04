package io.bluetape4k.examples.redisson.coroutines

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.redis.redisson.redissonClientOf
import io.bluetape4k.testcontainers.storage.RedisServer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.redisson.api.RedissonClient

abstract class AbstractRedissonCoroutineTest {

    companion object: KLogging() {

        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redissonClient by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson(connectionPoolSize = 256)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "$LibraryName:${Fakers.fixedString(32)}"

    }

    protected val redisson: RedissonClient get() = redissonClient

    protected fun newRedisson(): RedissonClient {
        val config = RedisServer.Launcher.RedissonLib.getRedissonConfig(
            connectionPoolSize = 256,
            minimumIdleSize = 12,
            threads = 64,
            nettyThreads = 256,
        ).apply {

        }
        return redissonClientOf(config)
    }

    protected val scope = CoroutineScope(CoroutineName("redisson") + Dispatchers.IO)

    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        log.error(exception) {
            "CoroutineExceptionHandler get exception with suppressed ${exception.suppressed.contentToString()} "
        }
        throw RuntimeException("Fail to execute in coroutine", exception)
    }
}
