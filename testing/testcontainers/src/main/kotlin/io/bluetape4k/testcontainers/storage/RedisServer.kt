package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


/**
 * Docker를 이용하여 Redis Server를 실행합니다.
 *
 * ```
 * val redisServer = RedisServer().apply {
 *      start()
 *      ShutdownQueue.register(this)
 * }
 * ```
 *
 * 참고: [Redis Docker image](https://hub.docker.com/_/redis)
 */
class RedisServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<RedisServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "redis"
        const val TAG = "7"
        const val NAME = "redis"
        const val PORT = 6379

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedisServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return RedisServer(imageName, useDefaultPort, reuse)
        }

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedisServer {
            return RedisServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)
    override val url: String get() = "$NAME://$host:$port"

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties(NAME)
    }

    object Launcher {

        val redis: RedisServer by lazy {
            RedisServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        object RedissonLib {
            // private val redissonClients = ConcurrentHashMap<String, RedissonClient>()

            fun getRedissonConfig(
                address: String = redis.url,
                connectionPoolSize: Int = 256,
                minimumIdleSize: Int = 24,
                threads: Int = 32,
                nettyThreads: Int = 128,
            ): Config {
                return Config().apply {
                    this.threads = threads
                    this.nettyThreads = nettyThreads
                    this.codec = this.codec ?: TEST_REDISSON_CODEC

                    with(useSingleServer()) {
                        this.address = address
                        this.connectionPoolSize = connectionPoolSize       // default: 64
                        this.connectionMinimumIdleSize = minimumIdleSize  // default: 24
                        this.retryAttempts = 3
                        this.setRetryDelay { Duration.ofMillis(it * 10L + 10L) }
                    }
                }
            }

            fun getRedisson(
                address: String = redis.url,
                connectionPoolSize: Int = 256,
                minimumIdleSize: Int = 24,
                threads: Int = 32,
                nettyThreads: Int = 128,
            ): RedissonClient {
                val config = getRedissonConfig(address, connectionPoolSize, minimumIdleSize, threads, nettyThreads)
                return Redisson.create(config)
                    .also { redisson ->
                        ShutdownQueue.register { redisson.shutdown() }
                    }
            }
        }

        object LettuceLib {

            private val redisClients = ConcurrentHashMap<String, RedisClient>()

            fun getRedisURI(host: String, port: Int): RedisURI {
                return RedisURI.builder()
                    .withHost(host)
                    .withPort(port)
                    .withTimeout(30.seconds.toJavaDuration())
                    .build()
            }

            fun getRedisClient(host: String = redis.host, port: Int = redis.port): RedisClient {
                val uri = getRedisURI(host, port)
                return redisClients.computeIfAbsent(uri.toString()) {
                    RedisClient.create(getRedisURI(host, port))
                        .also { redisClient ->
                            ShutdownQueue.register { redisClient.shutdown() }
                        }
                }
            }

            fun getRedisClient(url: String): RedisClient {
                return redisClients.computeIfAbsent(url) {
                    RedisClient.create(RedisURI.create(url))
                        .also { redisClient ->
                            ShutdownQueue.register { redisClient.shutdown() }
                        }
                }
            }

            fun getRedisCommands(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisCommands<String, String> = getRedisClient(host, port).connect().sync()

            fun getRedisAsyncCommands(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisAsyncCommands<String, String> = getRedisClient(host, port).connect().async()

            @OptIn(ExperimentalLettuceCoroutinesApi::class)
            fun getRedisCoroutinesCommands(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisCoroutinesCommands<String, String> = getRedisClient(host, port).connect().coroutines()


            fun <K: Any, V> getRedisCommands(
                host: String = redis.host,
                port: Int = redis.port,
                codec: RedisCodec<K, V>,
            ): RedisCommands<K, V> = getRedisClient(host, port).connect(codec).sync()


            fun <K: Any, V> getRedisAsyncCommands(
                host: String = redis.host,
                port: Int = redis.port,
                codec: RedisCodec<K, V>,
            ): RedisAsyncCommands<K, V> = getRedisClient(host, port).connect(codec).async()

            @OptIn(ExperimentalLettuceCoroutinesApi::class)
            fun <K: Any, V: Any> getRedisCoroutinesCommands(
                host: String = redis.host,
                port: Int = redis.port,
                codec: RedisCodec<K, V>,
            ): RedisCoroutinesCommands<K, V> = getRedisClient(host, port).connect(codec).coroutines()
        }
    }
}
