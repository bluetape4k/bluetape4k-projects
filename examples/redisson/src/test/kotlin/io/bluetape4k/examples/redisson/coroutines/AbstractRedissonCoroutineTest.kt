package io.bluetape4k.examples.redisson.coroutines

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.redis.redisson.redissonClientOf
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
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
        val redissonClient by lazy { newRedisson() }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "$LibraryName:${Fakers.fixedString(32)}"


        @JvmStatic
        protected fun newRedisson(): RedissonClient {
            val config = RedisServer.Launcher.RedissonLib.getRedissonConfig(
                connectionPoolSize = 256,
                minimumIdleSize = 12,
                threads = 128,
                nettyThreads = 512,
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
