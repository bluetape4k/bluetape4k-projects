package io.bluetape4k.examples.redisson.coroutines

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.support.classIsPresent
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import org.redisson.Redisson
import org.redisson.api.RFuture
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

abstract class AbstractRedissonCoroutineTest {

    companion object: KLoggingChannel() {

        @JvmStatic
        val redis: RedisServer by lazy {
            RedisServer(
                DockerImageName.parse(
                    "redis@sha256:4e070415a5713188624f93815e62d6c6a1fcbb416d2e0b578ab3db627db3a93a"
                )
            ).apply {
                start()
                ShutdownQueue.register(this)

                if (classIsPresent("org.redisson.Redisson")) {
                    val warmupClient = Redisson.create(
                        RedisServer.Launcher.RedissonLib.getRedissonConfig(url)
                    )
                    try {
                        RedisServer.Launcher.RedissonLib.warmupPubSubChannel(warmupClient)
                    } finally {
                        warmupClient.shutdown(0, 5, TimeUnit.SECONDS)
                    }
                }
            }
        }

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
        protected fun newRedisson(registerShutdown: Boolean = true): RedissonClient {
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

            return Redisson.create(config).also { client ->
                if (registerShutdown) {
                    ShutdownQueue.register { client.shutdown() }
                }
            }
        }
    }

    /**
     * Redisson 비동기 호출을 bounded coroutine suspension으로 소비한다.
     *
     * Timeout 또는 호출자 취소가 발생하면 아직 완료되지 않은 Redis future에
     * client-side [java.util.concurrent.Future.cancel]을 best-effort로 시도해
     * 테스트 종료 뒤의 pending wait를 줄인다. 이미 Redis 서버에 제출된 명령의
     * 원격 실행 취소까지 보장하는 helper는 아니며, 원래의 cancellation 원인은
     * 그대로 다시 던진다.
     */
    protected suspend fun <T> awaitRedis(
        future: RFuture<T>,
        timeout: kotlin.time.Duration = 5.seconds,
    ): T = try {
        withTimeout(timeout) { future.await() }
    } catch (cause: TimeoutCancellationException) {
        future.cancel(false)
        throw cause
    } catch (cause: CancellationException) {
        future.cancel(false)
        throw cause
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
