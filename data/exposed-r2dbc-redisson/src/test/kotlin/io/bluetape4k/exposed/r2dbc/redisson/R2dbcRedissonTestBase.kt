package io.bluetape4k.exposed.r2dbc.redisson

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.redis.redisson.redissonClientOf
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.redisson.api.RedissonClient

abstract class R2dbcRedissonTestBase: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel() {

        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redissonClient by lazy { newRedisson() }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "$LibraryName:${Base58.randomString(8)}"

        @JvmStatic
        protected fun newRedisson(): RedissonClient {
            val config = RedisServer.Launcher.RedissonLib.getRedissonConfig(
                connectionPoolSize = 256,
                minimumIdleSize = 12,
                threads = 24,
                nettyThreads = 64,
            )
            return redissonClientOf(config).apply {
                ShutdownQueue.register { shutdown() }
            }
        }
    }

    protected val redisson: RedissonClient get() = redissonClient

    protected val scope = CoroutineScope(CoroutineName("redisson") + Dispatchers.IO)

    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        log.error(exception) {
            "CoroutineExceptionHandler get exception with suppressed ${exception.suppressed.contentToString()} "
        }
        throw RuntimeException("Fail to execute in coroutine", exception)
    }
}
