package io.bluetape4k.examples.redisson.coroutines

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import java.time.Duration

abstract class AbstractRedissonCoroutineTest {

    companion object: KLoggingChannel() {

        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redissonClient by lazy { newRedisson() }

        @JvmStatic
        val defaultCodec = RedissonCodecs.ZstdFory

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "$LibraryName:${Base58.randomString(8)}"


        @JvmStatic
        protected fun newRedisson(): RedissonClient {
            val config = Config().apply {
                useSingleServer()
                    .setAddress(redis.url)
                    .setConnectionPoolSize(128)
                    .setConnectionMinimumIdleSize(32) // 최소 연결을 충분히 확보하여 Latency 방지
                    .setIdleConnectionTimeout(100_000)  // 연결 유지를 넉넉히 (100초)
                    .setTimeout(5000)
                    .setRetryAttempts(3)
                    .setRetryDelay { attempt -> Duration.ofMillis((attempt + 1) * 100L) }

                    .setDnsMonitoringInterval(5000)  // DNS 변경 감지 (Cloud 환경 필수)

                executor = VirtualThreadExecutor
                threads = 256
                nettyThreads = 128
                codec = RedissonCodecs.LZ4ForyComposite
                setTcpNoDelay(true)
                setTcpUserTimeout(5000)
            }

            return Redisson.create(config).apply {
                ShutdownQueue.register { shutdown() }
            } as Redisson
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
