package io.bluetape4k.testcontainers.storage

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.storage.RedisServer.Launcher.RedissonLib.getRedissonConfig
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
 * Redis 단일 노드 테스트 서버 컨테이너를 생성하고 클라이언트 헬퍼를 제공합니다.
 *
 * ## 동작/계약
 * - 인스턴스 생성만으로는 시작되지 않으며 `start()` 호출 후에만 접속할 수 있습니다.
 * - `useDefaultPort=true`이면 `6379` 포트 고정 바인딩을 시도하고, 아니면 동적 포트를 사용합니다.
 * - `url`은 `redis://host:port` 형식으로 계산됩니다.
 *
 * ```kotlin
 * val server = RedisServer()
 * server.start()
 * // server.url.startsWith("redis://") == true
 * ```
 *
 * 참고: [Redis Docker image](https://hub.docker.com/_/redis)
 */
class RedisServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<RedisServer>(imageName),
   GenericServer {
    companion object: KLogging() {
        const val IMAGE = "redis"
        const val TAG = "8"
        const val NAME = "redis"
        const val PORT = 6379

        /**
         * 이미지 이름/태그로 [RedisServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환한 뒤 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 수행하지 않습니다.
         *
         * ```kotlin
         * val server = RedisServer(image = "redis", tag = "8")
         * // server.port > 0
         * ```
         */
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

        /**
         * [DockerImageName]으로 [RedisServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("redis").withTag("8")
         * val server = RedisServer(image)
         * // server.isRunning == false
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedisServer = RedisServer(imageName, useDefaultPort, reuse)
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

    /**
     * 테스트 전역에서 재사용할 Redis 서버 싱글턴과 클라이언트 생성 유틸을 제공합니다.
     *
     * ## 동작/계약
     * - `redis`는 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - 각 헬퍼 함수는 필요 시 클라이언트/연결 객체를 새로 만들며 호출자가 연결 생명주기를 관리해야 합니다.
     *
     * ```kotlin
     * val server = RedisServer.Launcher.redis
     * val client = RedisServer.Launcher.LettuceLib.getRedisClient()
     * // server.isRunning == true
     * ```
     */
    object Launcher: KLogging() {
        /**
         * 지연 초기화되는 재사용용 Redis 서버입니다.
         */
        val redis: RedisServer by lazy {
            RedisServer()
                .apply {
                    start()
                    ShutdownQueue.register(this)

                    // Testcontainers 첫 실행 시 Docker 포트 프록시가 완전히 준비되기 전에
                    // Redisson pub/sub 채널을 열면 StacklessClosedChannelException이 발생합니다.
                    // 임시 클라이언트로 pub/sub를 워밍업하여 포트 프록시를 안정화합니다.
                    // 이후 Redisson.create()로 직접 생성한 클라이언트도 안전하게 사용할 수 있습니다.
                    val tempClient = RedissonLib.getRedisson(url)
                    try {
                        RedissonLib.warmupPubSubChannel(tempClient)
                    } finally {
                        tempClient.shutdown()
                    }
                }
        }

        /**
         * Redisson 설정/클라이언트 생성을 위한 헬퍼를 제공합니다.
         */
        object RedissonLib: KLogging() {
            /**
             * 단일 서버 모드 [Config]를 생성합니다.
             *
             * ## 동작/계약
             * - 매 호출마다 새 [Config]를 생성해 반환합니다.
             * - `address`는 검증 없이 그대로 사용되며 잘못된 URL이면 연결 시점에 실패합니다.
             * - 재시도/풀 크기 옵션을 테스트 기본값으로 채웁니다.
             *
             * ```kotlin
             * val config = RedisServer.Launcher.RedissonLib.getRedissonConfig()
             * // config.useSingleServer().address.startsWith("redis://") == true
             * ```
             */
            fun getRedissonConfig(
                address: String = redis.url,
                connectionPoolSize: Int = 256,
                minimumIdleSize: Int = 24,
                threads: Int = 256,
                nettyThreads: Int = 128,
            ): Config =
                Config().apply {
                    with(useSingleServer()) {
                        this.address = address
                        this.connectionPoolSize = connectionPoolSize // default: 64
                        this.connectionMinimumIdleSize = minimumIdleSize // default: 24
                        this.idleConnectionTimeout = 100_000 // 연결 유지를 넉넉히 (100초)
                        this.timeout = 5000
                        this.retryAttempts = 3
                        this.setRetryDelay { Duration.ofMillis(it * 100L + 100L) }
                    }

                    this.executor = VirtualThreadExecutor
                    this.threads = threads
                    this.nettyThreads = nettyThreads
                    this.codec = this.codec ?: TEST_REDISSON_CODEC
                    setTcpNoDelay(true)
                    // setTcpUserTimeout(5000)  // ← Linux 전용, Docker Desktop macOS에서 간헐적 채널 종료 유발
                }

            /**
             * Redisson 클라이언트를 생성하고 JVM 종료 시 shutdown 훅을 등록합니다.
             *
             * ## 동작/계약
             * - 내부적으로 [getRedissonConfig]를 호출해 새 [RedissonClient]를 생성합니다.
             * - 반환된 클라이언트는 자동 캐시하지 않으므로 호출할 때마다 새 인스턴스가 만들어집니다.
             * - 종료 훅은 [ShutdownQueue]에 등록됩니다.
             *
             * ```kotlin
             * val redisson = RedisServer.Launcher.RedissonLib.getRedisson()
             * // redisson.isShuttingDown == false
             * ```
             */
            fun getRedisson(
                address: String = redis.url,
                connectionPoolSize: Int = 256,
                minimumIdleSize: Int = 24,
                threads: Int = 32,
                nettyThreads: Int = 128,
            ): RedissonClient {
                val config = getRedissonConfig(address, connectionPoolSize, minimumIdleSize, threads, nettyThreads)
                return Redisson
                    .create(config)
                    .also { redisson ->
                        RedissonLib.warmupPubSubChannel(redisson)
                        ShutdownQueue.register { redisson.shutdown() }
                    }
            }

            /**
             * Testcontainers 첫 실행 시 Docker 포트 프록시 준비 전 pub/sub 채널 접근 시
             * StacklessClosedChannelException이 발생하므로 미리 워밍업합니다.
             */
            fun warmupPubSubChannel(client: RedissonClient, maxAttempts: Int = 5) {
                repeat(maxAttempts) { attempt ->
                    runCatching {
                        log.debug { "Warm up Pub/Sub channel (attempt ${attempt + 1}/$maxAttempts)" }
                        val topic = client.getPatternTopic("__warmup__*")
                        val listenerId = topic.addListener(Any::class.java) { _, _, _ -> }
                        topic.removeListener(listenerId)
                        return
                    }.onFailure { e ->
                        log.warn("pub/sub 워밍업 실패 (attempt ${attempt + 1}/$maxAttempts): ${e.message}")
                        Thread.sleep((attempt + 1) * 300L)
                    }
                }
            }
        }

        /**
         * Lettuce 클라이언트 생성/연결 헬퍼를 제공합니다.
         */
        object LettuceLib: KLogging() {
            private val redisClients = ConcurrentHashMap<String, RedisClient>()

            /**
             * 호스트/포트로 [RedisURI]를 생성합니다.
             *
             * ## 동작/계약
             * - timeout은 30초로 고정됩니다.
             * - 새 [RedisURI] 인스턴스를 반환하며 내부 상태는 변경하지 않습니다.
             *
             * ```kotlin
             * val uri = RedisServer.Launcher.LettuceLib.getRedisURI("localhost", 6379)
             * // uri.port == 6379
             * ```
             */
            fun getRedisURI(
                host: String,
                port: Int,
            ): RedisURI =
                RedisURI
                    .builder()
                    .withHost(host)
                    .withPort(port)
                    .withTimeout(30.seconds.toJavaDuration())
                    .build()

            /**
             * 호스트/포트 기준으로 [RedisClient]를 반환합니다.
             *
             * ## 동작/계약
             * - 내부 캐시 키(`uri.toString()`)가 있으면 기존 클라이언트를 재사용합니다.
             * - 없으면 새 클라이언트를 생성하고 [ShutdownQueue] 종료 훅을 등록합니다.
             *
             * ```kotlin
             * val client = RedisServer.Launcher.LettuceLib.getRedisClient()
             * // client != null
             * ```
             */
            fun getRedisClient(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisClient {
                val uri = getRedisURI(host, port)
                return redisClients.computeIfAbsent(uri.toString()) {
                    RedisClient
                        .create(getRedisURI(host, port))
                        .also { redisClient ->
                            ShutdownQueue.register { redisClient.shutdown() }
                        }
                }
            }

            /**
             * URL 문자열 기준으로 [RedisClient]를 반환합니다.
             *
             * ## 동작/계약
             * - 동일 URL이면 내부 캐시된 클라이언트를 재사용합니다.
             * - URL 형식이 잘못되면 `RedisURI.create`/연결 시점에 예외가 발생할 수 있습니다.
             *
             * ```kotlin
             * val client = RedisServer.Launcher.LettuceLib.getRedisClient("redis://localhost:6379")
             * // client != null
             * ```
             */
            fun getRedisClient(url: String): RedisClient =
                redisClients.computeIfAbsent(url) {
                    RedisClient
                        .create(RedisURI.create(url))
                        .also { redisClient ->
                            ShutdownQueue.register { redisClient.shutdown() }
                        }
                }

            /**
             * 기본 문자열 코덱 기반 동기 커맨드 API를 반환합니다.
             *
             * ## 동작/계약
             * - 내부적으로 새 connection을 열고 `sync()`를 반환합니다.
             * - connection close 책임은 호출자에게 있습니다.
             */
            fun getRedisCommands(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisCommands<String, String> = getRedisClient(host, port).connect().sync()

            /**
             * 기본 문자열 코덱 기반 비동기 커맨드 API를 반환합니다.
             *
             * ## 동작/계약
             * - 내부적으로 새 connection을 열고 `async()`를 반환합니다.
             * - connection close 책임은 호출자에게 있습니다.
             */
            fun getRedisAsyncCommands(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisAsyncCommands<String, String> = getRedisClient(host, port).connect().async()

            /**
             * 기본 문자열 코덱 기반 코루틴 커맨드 API를 반환합니다.
             *
             * ## 동작/계약
             * - 내부적으로 새 connection을 열고 `coroutines()` 어댑터를 반환합니다.
             * - 실험 API([ExperimentalLettuceCoroutinesApi])에 의존합니다.
             * - connection close 책임은 호출자에게 있습니다.
             */
            @OptIn(ExperimentalLettuceCoroutinesApi::class)
            fun getRedisCoroutinesCommands(
                host: String = redis.host,
                port: Int = redis.port,
            ): RedisCoroutinesCommands<String, String> = getRedisClient(host, port).connect().coroutines()

            /**
             * 커스텀 코덱 기반 동기 커맨드 API를 반환합니다.
             *
             * ## 동작/계약
             * - 전달한 `codec`으로 새 connection을 열어 `sync()`를 반환합니다.
             * - `codec`이 키/값 타입과 맞지 않으면 런타임 직렬화 오류가 발생할 수 있습니다.
             */
            fun <K: Any, V> getRedisCommands(
                host: String = redis.host,
                port: Int = redis.port,
                codec: RedisCodec<K, V>,
            ): RedisCommands<K, V> = getRedisClient(host, port).connect(codec).sync()

            /**
             * 커스텀 코덱 기반 비동기 커맨드 API를 반환합니다.
             *
             * ## 동작/계약
             * - 전달한 `codec`으로 새 connection을 열어 `async()`를 반환합니다.
             * - connection close 책임은 호출자에게 있습니다.
             */
            fun <K: Any, V> getRedisAsyncCommands(
                host: String = redis.host,
                port: Int = redis.port,
                codec: RedisCodec<K, V>,
            ): RedisAsyncCommands<K, V> = getRedisClient(host, port).connect(codec).async()

            /**
             * 커스텀 코덱 기반 코루틴 커맨드 API를 반환합니다.
             *
             * ## 동작/계약
             * - 전달한 `codec`으로 새 connection을 열어 `coroutines()`를 반환합니다.
             * - 실험 API([ExperimentalLettuceCoroutinesApi])에 의존합니다.
             * - connection close 책임은 호출자에게 있습니다.
             */
            @OptIn(ExperimentalLettuceCoroutinesApi::class)
            fun <K: Any, V: Any> getRedisCoroutinesCommands(
                host: String = redis.host,
                port: Int = redis.port,
                codec: RedisCodec<K, V>,
            ): RedisCoroutinesCommands<K, V> = getRedisClient(host, port).connect(codec).coroutines()
        }
    }
}
